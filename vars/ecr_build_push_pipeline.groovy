#!/usr/bin/env groovy

def call(body) {
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent { label 'SLAVE' }
        options {
            withAWS(credentials: 'cloudformation', region: 'us-east-1')
        }
        stages {
            stage('Clone Source Project') {
                steps {
                    script {
                        def appRepo = "${pipelineParams.appRepo}"
                        def appName = "${pipelineParams.appName}"
                    }
                    sh """
                    #!/bin/bash
                    rm -rf $appName | true
                    git clone $appRepo
                    """
                }
            }
            stage('Building Image'){
                steps{
                    sh """
                    #!/bin/bash
                    docker image rm myimage | true
                    docker build . -t myimage
                    """
                }
            }
            stage('Pushing Image'){
                steps{
                    script {
                        def region = "${pipelineParams.region}"
                        def accountId = "${pipelineParams.accountId}"
                    }
                    sh """
                    #!/bin/bash
                    aws ecr get-login-password --region $region | docker login --username AWS --password-stdin "$accountId.dkr.ecr.$region.amazonaws.com"
                    """
                }
            }
        }
    }
}