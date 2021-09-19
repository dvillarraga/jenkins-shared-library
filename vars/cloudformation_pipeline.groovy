#!/usr/bin/env groovy

def call(body) {
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        options {
            withAWS(credentials: 'cloudformation', region: 'us-east-1')
        }
        environment {
            stackName = "${pipelineParams.stackName}"
        }
        stages {
            stage('Template Validation') {
                steps {
                    script{
                        def response = cfnValidate(file:"${pipelineParams.templateFile}")
                        echo "Template Description: ${response.description}"    
                    }
                }
            }
        }
    }
}