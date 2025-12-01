FROM httpd:2.4

# Install the pre-packaged OpenID Connect module for Apache
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libapache2-mod-auth-openidc \
    ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/lib/apache2/modules/mod_auth_openidc.so /usr/local/apache2/modules/mod_auth_openidc.so

# # The httpd.conf file is mounted via docker-compose, so no COPY is needed here.
# # The modules are enabled via LoadModule directives in the mounted httpd.conf.

# # The httpd.conf file is mounted via docker-compose, so no COPY is needed here.
# # The modules are enabled via LoadModule directives in the mounted httpd.conf.