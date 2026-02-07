# Retry with backoff for webhook delivery. Computes delay (e.g. exponential
# backoff) between delivery attempts. Responsibility: Backoff schedule and jitter.

module WebhookService
  module Internal
    module Retry
    end
  end
end
