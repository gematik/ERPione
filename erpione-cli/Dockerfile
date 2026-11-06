FROM alpine/java:21-jre
#FROM bellsoft/liberica-openjdk-alpine:21-cds

ARG USER=erpione
ARG COMMIT_HASH
ARG VERSION
ARG BUILD_DATE

ENV HOME=/home/$USER
ENV VERSION=$VERSION
ENV COMMIT_HASH=$COMMIT_HASH
ENV BUILD_DATE=$BUILD_DATE
ENV PATH=$PATH:/usr/bin/erpione
ENV LC_CTYPE=en_US.UTF-8
ENV ERPIONE_ENV=RU

LABEL de.gematik.vendor="gematik GmbH"
LABEL maintainer="alexander.kaiser@gematik.de"
LABEL de.gematik.app="Erpione"
LABEL de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/git/erezept/erpione/erpione-application"
LABEL de.gematik.commit-sha=$COMMIT_HASH
LABEL de.gematik.version=$VERSION

# add new user
RUN adduser -D "$USER" \
    && mkdir -p -m777 /etc/bash-completion.d/ \
    && apk update  \
    && apk upgrade  \
    && apk add --no-cache \
                alpine-zsh-config curl git jq less zsh


COPY ./target/appassembler/ /usr/
COPY --chmod=755 ./src/main/resources/erpione-cli.json $HOME/.erpione/
COPY --chmod=755 ./target/test-classes/examples/ $HOME/examples/


RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v1.1.5/zsh-in-docker.sh)" --\
            -x \
            -t robbyrussell \
            -p https://github.com/zsh-users/zsh-history-substring-search \
            -p https://github.com/zsh-users/zsh-autosuggestions \
            -p https://github.com/zsh-users/zsh-completions \
    && erpione --generate-completion=zsh > /etc/bash-completion.d/erpione_completion.sh \
    && echo 'for bcfile in /etc/bash-completion.d/* ; do . ${bcfile}; done' >> /etc/zsh/zshrc \
    && echo 'ZSH_AUTOSUGGEST_STRATEGY=(completion history)' >> /etc/zsh/zshrc

USER $USER
WORKDIR $HOME

CMD ["/bin/zsh"]