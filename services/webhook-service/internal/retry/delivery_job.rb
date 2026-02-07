# Delivery job that performs HTTP POST to endpoint with retries.
# On max retries exceeded, moves to dead-letter. Responsibility: Execute delivery and retry logic.

module WebhookService
  module Internal
    module Retry
    end
  end
end
