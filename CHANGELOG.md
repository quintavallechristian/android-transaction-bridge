# Changelog

All notable changes to this project will be documented here.

## Unreleased

## 0.1.1 - 2026-09-01

- Prevent recognized notifications from being lost when durable queue or attention-log writes fail.
- Preserve separate Android notification posts while keeping webhook delivery retries idempotent.
- Expand parser, registry, persistence, and delivery regression coverage.
- Simplify shared record encoding and Android UI construction.

## 0.1.0 - 2026-08-31

- Initial public release.
- Parse supported Crypto.com, Google Wallet, ING, IsyBank, and Revolut payment notifications.
- Deliver versioned, deduplicated JSON to an HTTPS webhook with optional Bearer authentication.
- Queue temporary failures locally with bounded retry and keep non-retryable failures available for manual review.
