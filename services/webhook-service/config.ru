# Rack config for webhook-service. Mounts the Sinatra app and
# configures middleware. Responsibility: Application entrypoint for rackup/puma.

require_relative 'app'
run WebhookService::App
