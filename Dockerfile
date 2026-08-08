FROM eclipse-temurin:8-jdk-jammy AS build

WORKDIR /workspace
COPY ch.obermuhlner.math.big/src/main/java /workspace/ch.obermuhlner.math.big/src/main/java
COPY ch.obermuhlner.math.big.example/src/main/java /workspace/ch.obermuhlner.math.big.example/src/main/java

RUN mkdir -p /opt/big-math/classes \
    && find ch.obermuhlner.math.big/src/main/java ch.obermuhlner.math.big.example/src/main/java -name '*.java' -print0 \
    | xargs -0 javac -source 8 -target 8 -d /opt/big-math/classes

FROM eclipse-temurin:8-jre-jammy

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        fluxbox \
        novnc \
        websockify \
        x11vnc \
        xvfb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/big-math

COPY --from=build /opt/big-math/classes /opt/big-math/classes
COPY docker/run-gui.sh /usr/local/bin/run-gui.sh

RUN chmod +x /usr/local/bin/run-gui.sh

ENV DISPLAY=:99
ENV XVFB_WHD=1440x900x24

EXPOSE 8080 5900

CMD ["/usr/local/bin/run-gui.sh"]
