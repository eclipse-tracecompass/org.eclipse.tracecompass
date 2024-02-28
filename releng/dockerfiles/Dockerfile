# Copyright (c) 2023, 2024 Polytechnique de Montréal
#
# All rights reserved. This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0 which
# accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

FROM alpine:3.18.0 as packager

RUN apk --no-cache add openjdk17-jdk openjdk17-jmods maven

COPY ./ /app/

WORKDIR /app/

RUN mvn clean install -DskipTests -Dskip-rcp -Pfilter-grammar -Pctf-grammar

FROM alpine:3.18.0

COPY --from=packager /root/.m2/ /root/.m2/

