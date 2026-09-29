# Build context is the repository root (Cargo workspace), e.g.:
#   docker build -f services/operator/Dockerfile .
FROM rust:1-bookworm AS builder
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/src/target \
    cargo build --release --locked -p operator \
    && cp target/release/operator /operator

FROM gcr.io/distroless/cc-debian12:nonroot
COPY --from=builder /operator /usr/local/bin/operator
ENTRYPOINT ["/usr/local/bin/operator"]
