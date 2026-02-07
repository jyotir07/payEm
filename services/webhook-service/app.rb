# Application entrypoint for webhook-service. Defines the Sinatra app,
# routes, and wiring for delivery, signing, retry, and dead-letter.
# Responsibility: Bootstrap the webhook service and register endpoints.

require 'sinatra/base'

module WebhookService
  class App < Sinatra::Base
    # Routes and middleware for webhook delivery and status.
  end
end
