FROM java
ENV PATH="/hsqlman:${PATH}"
RUN wget https://github.com/IvoFritsch/hsqldb-manager/raw/master/hsqldb-manager.zip
RUN unzip hsqldb-manager.zip -d /hsqlman
WORKDIR /hsqlman
RUN rm /hsqlman/acl.txt
RUN echo "{\"db\":{\"name\":\"db\",\"path\":\"/var/db\"}}" > /hsqlman/deployed_dbs.db
RUN chmod +x /hsqlman/hsqlman
RUN cat /hsqlman/hsqlman
ENTRYPOINT ["java", "-jar", "./hsqldb-manager.jar", "--no-acl"]
EXPOSE 7030 1111 35888
