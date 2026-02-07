# Service container definition for webhook-service. Multi-stage or single-stage
# build: install gems, run app via puma/sinatra. Responsibility: Production and local dev image.

FROM ruby:3.2-alpine
WORKDIR /app
RUN apk add --no-cache build-base
COPY Gemfile Gemfile.lock ./
RUN bundle config set --local deployment 'true' && bundle install --without development test
COPY . .
EXPOSE 9292
CMD ["bundle", "exec", "puma", "-p", "9292"]
