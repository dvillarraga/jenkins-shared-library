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
        stages {
            stage('Template Validation') {
                steps {
                    script{
                        def response = cfnValidate(file:"${pipelineParams.templateFile}")
                        echo "Template Description: ${response.description}"
                    }
                }
            }
            stage('Deploying Stack'){
                steps{
                    script{
                        def deployStack = cfnUpdate(stack:"${pipelineParams.stackName}", file:"${pipelineParams.templateFile}", params:["AppTag=${pipelineParams.appTag}"], timeoutInMinutes:120, tags:["app=${pipelineParams.appTag}"], pollInterval:1000)
                    }
                }
            }
        }
    }
}