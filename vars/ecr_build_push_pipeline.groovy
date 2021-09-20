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
                        def branch = "${pipelineParams.branch}"
                    }
                    sh """
                    #!/bin/bash
                    rm -rf app | true
                    mkdir -p app
                    cd app
                    git clone $appRepo
                    git checkout $branch
                    """
                }
            }
            stage('Building Image'){
                script{
                    def repoName = "${pipelineParams.repoName}"
                }
                steps{
                    sh """
                    #!/bin/bash
                    docker image rm $repoName | true
                    docker build -t $repoName .
                    """
                }
            }
            stage('Pushing Image'){
                steps{
                    script {
                        def region = "${pipelineParams.region}"
                        def repoUri = "${pipelineParams.repoUri}"
                        def repoName = "${pipelineParams.repoName}"
                    }
                    sh """
                    #!/bin/bash
                    aws ecr get-login-password --region $region | docker login --username AWS --password-stdin $repoUri
                    docker tag $repoName:latest $repoUri/$repoName:latest
                    docker push $repoUri/$repoName:latest
                    """
                }
            }
        }
    }
}