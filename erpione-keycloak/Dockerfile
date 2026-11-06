FROM quay.io/keycloak/keycloak:latest AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

WORKDIR /opt/keycloak
RUN /opt/keycloak/bin/kc.sh build --http-relative-path /kc

FROM quay.io/keycloak/keycloak:latest
ENV LANG=en_US.UTF-8

COPY --from=builder /opt/keycloak/ /opt/keycloak/


ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# common gematik labels
ARG COMMIT_HASH
ARG VERSION
ARG BUILD_DATE

LABEL de.gematik.vendor="gematik GmbH"
LABEL maintainer="alexander.kaiser@gematik.de"
LABEL de.gematik.app="Erpione Auth Server"
LABEL de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/git/erezept/erpione/erpione-application"
LABEL de.gematik.commit-sha=$COMMIT_HASH
LABEL de.gematik.version=$VERSION