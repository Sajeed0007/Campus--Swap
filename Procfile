# Elastic Beanstalk (Java SE / Corretto platform) process definition.
#
# Beanstalk's nginx proxy forwards to port 5000, and newer platform versions
# export PORT. application.yml defaults server.port to 8080, which is correct for
# local and container runs, so it is overridden here rather than changed globally.
web: java -Dserver.port=${PORT:-5000} -XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -jar application.jar
