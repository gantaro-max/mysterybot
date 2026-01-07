# 1. ビルドを行う環境 (JDK 21)
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY . .
# Gradleでビルドを実行 (テストはスキップして高速化)
RUN ./gradlew bootJar -x test

# 2. 実際に動かす環境 (JRE 21 - 軽量版)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# ビルドした成果物(jarファイル)をコピー
COPY --from=builder /app/build/libs/*.jar app.jar

# ポート8080を開放
EXPOSE 8080

# アプリ起動コマンド
ENTRYPOINT ["java", "-jar", "app.jar"]