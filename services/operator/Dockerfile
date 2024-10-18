FROM rust:latest as builder
WORKDIR /build/
ENV DEBIAN_FRONTEND=noninteractive
# Install protoc
RUN apt-get update && apt-get install -y protobuf-compiler
# Copy project files
COPY . .
# Build the project
RUN --mount=type=cache,target=/build/target,sharing=locked \
    --mount=type=cache,target=/usr/local/cargo/registry,sharing=locked \
    cargo build --release --bin nautilus-operator \
    && cp target/release/nautilus-operator /build/nautilus-operator

FROM alpine
LABEL org.opencontainers.image.source=https://github.com/Phyrone/nautilus
LABEL org.opencontainers.image.licenses=EUPL-1.2
WORKDIR /app/
# Copy the built binary
COPY --from=builder /build/nautilus-operator .
