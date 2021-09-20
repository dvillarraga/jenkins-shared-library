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
                    }
                    sh """
                    #!/bin/bash
                    git clone $appRepo
                    """
                }
            }
        }
    }
}