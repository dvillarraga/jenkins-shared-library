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
            stage('Deploying Stack'){
                steps{
                    script{
                        try {
                            def describeStack = cfnDescribe(stack:"${pipelineParams.stackName}")
                        } catch (err) {
                            if(err.contains("Stack with id") && err.contains("does not exist")){
                                echo "All ok, lets create the stack"
                            }else{
                                echo "Ops, something failed"
                                echo "Caught: ${err}"
                                currentBuild.result = 'FAILURE'
                            }
                        }
                        
                    }
                }

            }
        }
    }
}