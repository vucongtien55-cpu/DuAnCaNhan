# Image Jenkins tùy chỉnh (theo slide "Cài đặt và cấu hình Jenkins trên Docker")
# - Nền: Jenkins LTS chạy trên JDK 21 (trùng phiên bản Java của dự án QuanLyChiTieu)
# - Cài thêm Docker CLI để Jenkins điều khiển container docker:dind (nếu cần)
# Build:  docker build -t myjenkins:2.580.2-1 .
FROM jenkins/jenkins:2.580.2-jdk21

USER root
RUN apt-get update && apt-get install -y lsb-release ca-certificates curl && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc && \
    chmod a+r /etc/apt/keyrings/docker.asc && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
    https://download.docker.com/linux/debian $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && apt-get install -y docker-ce-cli && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

USER jenkins
RUN jenkins-plugin-cli --plugins "docker-workflow json-path-api"
