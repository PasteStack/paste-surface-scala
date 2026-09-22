# Contributing

Thank you for your interest in contributing to **paste-surface-scala**.

This project is part of **Tomshley LLC's OSS and IP strategy** and is part of PasteStack,
which augments the tools a consumer already uses rather than taking over its application
model. It is intentionally opinionated.

## Philosophy

- Immutable view models, small total functions, explicit validation at construction
- Twirl templates that render markup and nothing else: no runtime state, no inline scripts
- The Element owns behaviour; the renderer owns markup

Changes that increase complexity, size, or scope without strong justification are unlikely
to be accepted.

## What Contributions Are Welcome

- Bug fixes
- Renderers for contracts published in `paste-surface-spec`
- Renderer checks for existing components
- Documentation improvements

## What Is Out of Scope

- Application policy, routing, or domain logic
- Renderers that emit configuration no Element reads, unless documented as reserved
- Changes that contradict the project's architecture or philosophy
- Large refactors without prior discussion

## Process

1. Open an issue describing the change
2. Fork the repository
3. Create a focused merge request
4. Keep commits clean and scoped

Tomshley LLC reserves final decision-making authority on all contributions.
