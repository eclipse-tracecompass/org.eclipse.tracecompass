/*******************************************************************************
 * Copyright (c) 2024 Ericsson.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
pipeline {
    agent {
        kubernetes {
            label 'tracecompass-build'
            yamlFile 'releng/jenkins/pod-templates/tracecompass-pod.yaml'
        }
    }
    options {
        timestamps()
        timeout(time: 4, unit: 'HOURS')
        disableConcurrentBuilds()
    }
    tools {
        maven 'apache-maven-3.9.5'
        jdk 'openjdk-jdk17-latest'
    }
    environment {
        MAVEN_OPTS="-Xms768m -Xmx4096m -XX:+UseSerialGC"
        TEST_OPTS="-Dskip-tc-core-tests=true -Dskip-short-tc-ui-tests=true  -Dskip-rcp=true -Djdk.version=17 -Djdk.release=17"
    }
    stages {
        stage('Build') {
            steps {
                container('tracecompass') {
                    sh 'mvn clean install -B -Pctf-grammar -Pbuild-rcp -Dmaven.repo.local=/home/jenkins/.m2/repository --settings /home/jenkins/.m2/settings.xml ${MAVEN_ARGS} ${TEST_OPTS}'
                }
            }
            post {
                always {
                    container('tracecompass') {
                        junit '*/*/target/surefire-reports/*.xml'
                        archiveArtifacts artifacts: '*/*tests/screenshots/*.jpeg,*/*tests/target/work/data/.metadata/.log', excludes: '**/org.eclipse.tracecompass.common.core.log', allowEmptyArchive: true
                    }
                }
            }
        }
    }
}
