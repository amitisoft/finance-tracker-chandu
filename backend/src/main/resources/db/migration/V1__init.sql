create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp with time zone not null,
    revoked boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_refresh_tokens_user foreign key (user_id) references users(id)
);

create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp with time zone not null,
    used boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_password_reset_tokens_user foreign key (user_id) references users(id)
);

create table accounts (
    id uuid primary key,
    user_id uuid not null,
    name varchar(100) not null,
    type varchar(30) not null,
    opening_balance decimal(12,2) not null default 0,
    current_balance decimal(12,2) not null default 0,
    institution_name varchar(120),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_accounts_user foreign key (user_id) references users(id)
);

create table categories (
    id uuid primary key,
    user_id uuid not null,
    name varchar(100) not null,
    type varchar(20) not null,
    color varchar(20),
    icon varchar(50),
    is_archived boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_categories_user foreign key (user_id) references users(id),
    constraint uk_categories_user_name_type unique (user_id, name, type)
);

create table budgets (
    id uuid primary key,
    user_id uuid not null,
    category_id uuid not null,
    budget_month int not null,
    budget_year int not null,
    amount decimal(12,2) not null,
    alert_threshold_percent int not null default 80,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_budgets_user foreign key (user_id) references users(id),
    constraint fk_budgets_category foreign key (category_id) references categories(id),
    constraint uk_budgets_scope unique (user_id, category_id, budget_month, budget_year)
);

create table goals (
    id uuid primary key,
    user_id uuid not null,
    linked_account_id uuid,
    name varchar(120) not null,
    target_amount decimal(12,2) not null,
    current_amount decimal(12,2) not null default 0,
    target_date date,
    icon varchar(50),
    color varchar(20),
    status varchar(30) not null default 'ACTIVE',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_goals_user foreign key (user_id) references users(id),
    constraint fk_goals_account foreign key (linked_account_id) references accounts(id)
);

create table recurring_transactions (
    id uuid primary key,
    user_id uuid not null,
    title varchar(120) not null,
    type varchar(20) not null,
    amount decimal(12,2) not null,
    category_id uuid,
    account_id uuid not null,
    frequency varchar(20) not null,
    start_date date not null,
    end_date date,
    next_run_date date not null,
    auto_create_transaction boolean not null default true,
    paused boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_recurring_user foreign key (user_id) references users(id),
    constraint fk_recurring_category foreign key (category_id) references categories(id),
    constraint fk_recurring_account foreign key (account_id) references accounts(id)
);

create table transactions (
    id uuid primary key,
    user_id uuid not null,
    account_id uuid not null,
    destination_account_id uuid,
    category_id uuid,
    recurring_transaction_id uuid,
    type varchar(20) not null,
    amount decimal(12,2) not null,
    transaction_date date not null,
    merchant varchar(200),
    note text,
    payment_method varchar(50),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_transactions_user foreign key (user_id) references users(id),
    constraint fk_transactions_account foreign key (account_id) references accounts(id),
    constraint fk_transactions_destination_account foreign key (destination_account_id) references accounts(id),
    constraint fk_transactions_category foreign key (category_id) references categories(id),
    constraint fk_transactions_recurring foreign key (recurring_transaction_id) references recurring_transactions(id)
);

create table transaction_tags (
    transaction_id uuid not null,
    tag varchar(60) not null,
    constraint fk_transaction_tags_transaction foreign key (transaction_id) references transactions(id)
);

create table account_members (
    id uuid primary key,
    account_id uuid not null,
    user_id uuid not null,
    invited_by_user_id uuid not null,
    role varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_account_members_account foreign key (account_id) references accounts(id),
    constraint fk_account_members_user foreign key (user_id) references users(id),
    constraint fk_account_members_invited_by foreign key (invited_by_user_id) references users(id),
    constraint uk_account_members_account_user unique (account_id, user_id)
);

create table rules (
    id uuid primary key,
    user_id uuid not null,
    condition_field varchar(30) not null,
    condition_operator varchar(30) not null,
    condition_value varchar(255) not null,
    action_type varchar(30) not null,
    action_value varchar(255) not null,
    active boolean not null default true,
    priority int not null default 100,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_rules_user foreign key (user_id) references users(id)
);

create table account_members (
    id uuid primary key,
    account_id uuid not null,
    user_id uuid not null,
    invited_by_user_id uuid not null,
    role varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_account_members_account foreign key (account_id) references accounts(id),
    constraint fk_account_members_user foreign key (user_id) references users(id),
    constraint fk_account_members_invited_by foreign key (invited_by_user_id) references users(id),
    constraint uk_account_members_account_user unique (account_id, user_id)
);

create table rules (
    id uuid primary key,
    user_id uuid not null,
    condition_field varchar(30) not null,
    condition_operator varchar(30) not null,
    condition_value varchar(255) not null,
    action_type varchar(30) not null,
    action_value varchar(255) not null,
    active boolean not null default true,
    priority int not null default 100,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_rules_user foreign key (user_id) references users(id)
);
