# LangChain4j model capability notes

## Current project baseline

- Project BOM keeps `langchain4j.version` at `0.35.0`.
- Local Maven repository is configured at `/Users/xiongxiao/Workspace/Idea/maven/repo`.
- `langchain4j-core-0.35.0.jar` contains:
  - `dev.langchain4j.model.chat.ChatLanguageModel`
  - `dev.langchain4j.model.chat.StreamingChatLanguageModel`
  - `dev.langchain4j.model.embedding.EmbeddingModel`
  - `dev.langchain4j.model.image.ImageModel`
  - `dev.langchain4j.model.language.LanguageModel`
  - `dev.langchain4j.model.language.StreamingLanguageModel`
  - `dev.langchain4j.model.moderation.ModerationModel`
  - `dev.langchain4j.model.scoring.ScoringModel`
  - multimodal message content classes: `ImageContent`, `AudioContent`, `VideoContent`, `PdfFileContent`, `TextContent`
- `langchain4j-core-0.35.0.jar` does not contain `dev.langchain4j.model.chat.ChatModel` or `dev.langchain4j.model.chat.StreamingChatModel`.

## Current high-version documentation

- Context7 selected `/langchain4j/langchain4j`, with available versions including `1.12.1`.
- Current docs use `dev.langchain4j.model.chat.ChatModel` and `StreamingChatModel`; docs state old `ChatLanguageModel` / `StreamingChatLanguageModel` names have been replaced.
- HTTPS Maven Central jar checks for `langchain4j-core` `1.7.1`, `1.11.0`, and `1.12.1` found only:
  - `dev.langchain4j.model.chat.ChatModel`
  - `dev.langchain4j.model.chat.StreamingChatModel`
  and did not find `ChatLanguageModel` / `StreamingChatLanguageModel`.
- Current docs still describe specialized model interfaces: `EmbeddingModel`, `ImageModel`, `ModerationModel`, and `ScoringModel`.
- Current docs describe `LanguageModel` as becoming obsolete in favor of chat APIs.
- Current multimodal examples send image/audio/video/PDF content through chat messages, not as independent `AUDIO` or `VIDEO` model types.

## Documentation implication for this change

- For this repository, keep public enum types aligned to the 0.35.0 abstraction layer already implemented: `CHAT`, `STREAMING_CHAT`, `EMBEDDING`, `IMAGE`, `LANGUAGE`, `STREAMING_LANGUAGE`, `MODERATION`, `SCORING`.
- In docs, explain that `CHAT` maps to 0.35.0 `ChatLanguageModel` and high versions rename it to `ChatModel`.
- Do not add `AUDIO`, `VIDEO`, or `PDF` model types for 0.35.0; describe them as multimodal `ChatMessage` content where supported by the concrete provider/model.
- Keep provider adapter exposure limited to locally verified 0.35.0 builder classes; mention high versions have broader provider support and renamed chat interfaces.
