rsync -ahp --exclude "*.xml" company-service/build/exploded-company-service/WEB-INF build/exploded-gae-microservices
rsync -ahp --exclude "*.xml" employees-service/build/exploded-employees-service/WEB-INF build/exploded-gae-microservices
