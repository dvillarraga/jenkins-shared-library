#!/usr/bin/env groovy

def call(body) {
    def pipelineParams= [:]
    def appRepo = ""
    def branchCheckout = ""
    def repoName = ""
    def region = ""
    def repoUri = ""
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    appRepo = pipelineParams.appRepo
    branchCheckout = pipelineParams.branchCheckout
    repoName = pipelineParams.repoName
    region = pipelineParams.region
    repoUri = pipelineParams.repoUri

    pipeline {
        agent { label 'SLAVE' }
        options {
            withAWS(credentials: 'cloudformation', region: 'us-east-1')
        }
        stages {
            stage('Clone Source Project') {
                steps {
                    script{
                        sh """
                        #!/bin/bash
                        rm -rf app | true
                        mkdir -p app
                        cd app
                        git clone $appRepo
                        cd \$(ls -d */|head -n 1)
                        git checkout $branchCheckout
                        """
                    }
                }
            }
            stage('Building Image'){
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
                    sh """
                    #!/bin/bash
                    aws ecr get-login-password --region $region | docker login --username AWS --password-stdin $repoUri
                    cd app
                    cd \$(ls -d */|head -n 1)
                    versionApp=\$(<.version)
                    docker tag $repoName:latest $repoUri/$repoName:\$versionApp
                    docker push $repoUri/$repoName:$versionApp
                    """
                }
            }
        }
    }
}