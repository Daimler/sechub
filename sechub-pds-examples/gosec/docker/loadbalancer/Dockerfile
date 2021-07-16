# SPDX-License-Identifier: MIT

FROM alpine:3.14

RUN apk update && \
    apk add nginx

# copy configuration script
RUN rm /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/nginx.conf

# copy run script into container
COPY run.sh /run.sh
RUN chmod +x /run.sh

# switch from root to non-root user
USER nginx

CMD ["/run.sh"]