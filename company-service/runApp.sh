dev_appserver.sh --address=0.0.0.0 --port=8080 --disable_update_check --jvm_flag=-Xdebug --jvm_flag=-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 build/exploded-company-service

