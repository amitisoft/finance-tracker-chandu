package com.hackathon.finance.service;

import com.hackathon.finance.dto.goal.GoalContributionRequest;
import com.hackathon.finance.dto.goal.GoalRequest;
import com.hackathon.finance.dto.goal.GoalResponse;
import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.GoalEntity;
import com.hackathon.finance.entity.enums.GoalStatus;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.GoalRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final AccountService accountService;
    private final UserContextService userContextService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals() {
        return goalRepository.findAllByUserOrderByCreatedAtDesc(userContextService.getCurrentUser()).stream().map(mapper::toGoalResponse).toList();
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        GoalEntity goal = new GoalEntity();
        goal.setUser(userContextService.getCurrentUser());
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setIcon(request.icon());
        goal.setColor(request.color());
        goal.setLinkedAccount(request.linkedAccountId() != null ? accountService.findOwned(request.linkedAccountId()) : null);
        goalRepository.save(goal);
        return mapper.toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse update(UUID id, GoalRequest request) {
        GoalEntity goal = findOwned(id);
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setIcon(request.icon());
        goal.setColor(request.color());
        goal.setLinkedAccount(request.linkedAccountId() != null ? accountService.findOwned(request.linkedAccountId()) : null);
        return mapper.toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse contribute(UUID id, GoalContributionRequest request) {
        GoalEntity goal = findOwned(id);
        AccountEntity account = request.sourceAccountId() != null ? accountService.findOwned(request.sourceAccountId()) : goal.getLinkedAccount();
        if (account != null) {
            if (account.getCurrentBalance().compareTo(request.amount()) < 0) {
                throw new BadRequestException("Insufficient balance for goal contribution.");
            }
            account.setCurrentBalance(account.getCurrentBalance().subtract(request.amount()));
        }
        goal.setCurrentAmount(goal.getCurrentAmount().add(request.amount()));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
        return mapper.toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse withdraw(UUID id, GoalContributionRequest request) {
        GoalEntity goal = findOwned(id);
        if (goal.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Cannot withdraw more than the saved amount.");
        }
        goal.setCurrentAmount(goal.getCurrentAmount().subtract(request.amount()));
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.ACTIVE);
        }
        if (request.sourceAccountId() != null) {
            AccountEntity account = accountService.findOwned(request.sourceAccountId());
            account.setCurrentBalance(account.getCurrentBalance().add(request.amount()));
        }
        return mapper.toGoalResponse(goal);
    }

    @Transactional(readOnly = true)
    public GoalEntity findOwned(UUID id) {
        return goalRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Goal not found."));
    }
}
