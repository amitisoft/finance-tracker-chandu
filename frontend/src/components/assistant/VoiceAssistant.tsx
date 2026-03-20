import { useMutation } from "@tanstack/react-query";
import { useEffect, useMemo, useRef, useState } from "react";
import { financeApi } from "../../services/financeApi";
import { useAuthStore } from "../../store/authStore";

type AssistantMessage = {
  id: number;
  role: "assistant" | "user";
  text: string;
};

type SpeechRecognitionShape = {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onresult: ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
};

declare global {
  interface Window {
    SpeechRecognition?: new () => SpeechRecognitionShape;
    webkitSpeechRecognition?: new () => SpeechRecognitionShape;
  }
}

let nextAssistantMessageId = 1;

export function VoiceAssistant() {
  const hydrated = useAuthStore((state) => state.hydrated);
  const accessToken = useAuthStore((state) => state.accessToken);
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [listening, setListening] = useState(false);
  const [messages, setMessages] = useState<AssistantMessage[]>([
    {
      id: nextAssistantMessageId++,
      role: "assistant",
      text: "Ask me to add an expense, tell you an account balance, remaining budget, or weekly or monthly expenses."
    }
  ]);
  const recognitionRef = useRef<SpeechRecognitionShape | null>(null);

  const mutation = useMutation({
    mutationFn: (message: string) => financeApi.assistantMessage({ message }),
    onSuccess: (response, message) => {
      setMessages((current) => [
        ...current,
        { id: nextAssistantMessageId++, role: "user", text: message },
        { id: nextAssistantMessageId++, role: "assistant", text: response.reply }
      ]);
      if ("speechSynthesis" in window) {
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(new SpeechSynthesisUtterance(response.spokenReply || response.reply));
      }
      setInput("");
    }
  });

  const supportsVoice = useMemo(() => Boolean(window.SpeechRecognition || window.webkitSpeechRecognition), []);

  useEffect(() => {
    if (!supportsVoice || recognitionRef.current) {
      return;
    }
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
      return;
    }
    const recognition = new Recognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = "en-IN";
    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript.trim();
      setInput(transcript);
      setListening(false);
      if (hydrated && accessToken) {
        mutation.mutate(transcript);
      }
    };
    recognition.onerror = () => {
      setListening(false);
    };
    recognition.onend = () => {
      setListening(false);
    };
    recognitionRef.current = recognition;
  }, [accessToken, hydrated, mutation, supportsVoice]);

  const submit = () => {
    const message = input.trim();
    if (!message || !hydrated || !accessToken) {
      return;
    }
    mutation.mutate(message);
  };

  return (
    <div className={open ? "assistant-widget open" : "assistant-widget"}>
      {open ? (
        <div className="assistant-panel">
          <div className="assistant-header">
            <div>
              <strong>Voice Assistant</strong>
              <span>Expense capture and finance Q&A</span>
            </div>
            <button className="assistant-icon-button" onClick={() => setOpen(false)} type="button">
              x
            </button>
          </div>

          <div className="assistant-messages">
            {messages.slice(-6).map((message) => (
              <div className={message.role === "assistant" ? "assistant-bubble" : "assistant-bubble user"} key={message.id}>
                {message.text}
              </div>
            ))}
          </div>

          <div className="assistant-inputs">
            <textarea
              className="assistant-textarea"
              onChange={(event) => setInput(event.target.value)}
              placeholder="Try: I ate biryani at XYZ restaurant for 100 rupees"
              rows={3}
              value={input}
            />
            <div className="assistant-actions">
              <button
                className="secondary-button"
                disabled={!supportsVoice || listening || !hydrated || !accessToken}
                onClick={() => {
                  recognitionRef.current?.start();
                  setListening(true);
                }}
                type="button"
              >
                {listening ? "Listening..." : "Mic"}
              </button>
              <button
                className="primary-button"
                disabled={mutation.isPending || !input.trim() || !hydrated || !accessToken}
                onClick={submit}
                type="button"
              >
                {mutation.isPending ? "Thinking..." : "Send"}
              </button>
            </div>
          </div>
        </div>
      ) : (
        <button className="assistant-launcher" onClick={() => setOpen(true)} type="button">
          Voice Assistant
        </button>
      )}
    </div>
  );
}
