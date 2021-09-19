def call(Map pipelineParams) {
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