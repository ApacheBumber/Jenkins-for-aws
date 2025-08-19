/******************
 ** CREATED BY LUAN COSTA **

 ******************/

properties([
  parameters([
    choice(name: 'ACCOUNT', choices: ['DEV', HML', 'PRD', ''], description: 'Escolha a conta *'),
    choice(name: 'REGION', choices: ['us-east-1'], description: 'Escolha a região *'),
    string(name: 'Description', defaultValue: '', description: 'Descreva o que ocorre no EventBridge'),
    string(name: 'NOME_RULE', defaultValue: '', description: 'Nome da regra do EventBridge'),
    string(name: 'CRON_EXPRESSION', defaultValue: 'cron(0 12 * * ? *)', description: 'Expressão CRON ou RATE (ex: cron(0 12 * * ? *))'),
    choice(name: 'COUNT', choices: ['1', '2', '3'], description: 'Escolha quantidade de Regras *'),

    // Rule 1
    choice(name: 'STATE_1', choices: ['ENABLED', 'DISABLED'], description: 'Estado da Regra 1'),
    choice(name: 'TARGET_TYPE_1', choices: [
      'Amazon Redshift', 'API Gateway', 'AppSync', 'Batch job queue', 'CloudWatch log group',
      'CodeBuild project', 'CodePipeline', 'EBS Create Snapshot', 'EC2 ImageBuilder',
      'EC2 RebootInstances API call', 'EC2 StopInstances API call', 'EC2 TerminateInstances API call',
      'ECS task', 'Firehose stream', 'Glue workflow', 'Incident Manager response plan',
      'Inspector assessment template', 'Kinesis stream', 'Lambda function', 'SageMaker Pipeline',
      'SNS topic', 'SQS queue', 'Step Functions state machine',
      'Systems Manager Automation', 'Systems Manager OpsItem', 'Systems Manager Run Command'
    ], description: 'Tipo de target do EventBridge'),

    string(name: 'TARGET_ARN_1', defaultValue: '', description: 'ARN do alvo da regra (SNS/SQS/Lambda/...)'),

    // Rule 2
    choice(name: 'STATE_2', choices: ['ENABLED', 'DISABLED'], description: 'Estado da Regra 2'),
    choice(name: 'TARGET_TYPE_2', choices: [
      'Amazon Redshift', 'API Gateway', 'AppSync', 'Batch job queue', 'CloudWatch log group',
      'CodeBuild project', 'CodePipeline', 'EBS Create Snapshot', 'EC2 ImageBuilder',
      'EC2 RebootInstances API call', 'EC2 StopInstances API call', 'EC2 TerminateInstances API call',
      'ECS task', 'Firehose stream', 'Glue workflow', 'Incident Manager response plan',
      'Inspector assessment template', 'Kinesis stream', 'Lambda function', 'SageMaker Pipeline',
      'SNS topic', 'SQS queue', 'Step Functions state machine',
      'Systems Manager Automation', 'Systems Manager OpsItem', 'Systems Manager Run Command'
    ], description: 'Tipo de target do EventBridge'),

    string(name: 'TARGET_ARN_2', defaultValue: '', description: 'ARN do alvo da regra (SNS/SQS/Lambda/...)'),

    // Rule 3
    choice(name: 'STATE_3', choices: ['ENABLED', 'DISABLED'], description: 'Estado da Regra 3'),
        choice(name: 'TARGET_TYPE_3', choices: [
      'Amazon Redshift', 'API Gateway', 'AppSync', 'Batch job queue', 'CloudWatch log group',
      'CodeBuild project', 'CodePipeline', 'EBS Create Snapshot', 'EC2 ImageBuilder',
      'EC2 RebootInstances API call', 'EC2 StopInstances API call', 'EC2 TerminateInstances API call',
      'ECS task', 'Firehose stream', 'Glue workflow', 'Incident Manager response plan',
      'Inspector assessment template', 'Kinesis stream', 'Lambda function', 'SageMaker Pipeline',
      'SNS topic', 'SQS queue', 'Step Functions state machine',
      'Systems Manager Automation', 'Systems Manager OpsItem', 'Systems Manager Run Command'
    ], description: 'Tipo de target do EventBridge'),

    string(name: 'TARGET_ARN_3', defaultValue: '', description: 'ARN do alvo da regra (SNS/SQS/Lambda/...)'),
    choice(name: 'TAGENV', choices: ['dev','hml','prd','preprd','beta'], description: 'Escolha da TAG DE AMBIENTE'),
    choice(name: 'TAGCC', choices: [ ], description: 'Escolha da TAG DE CENTRO DE CUSTO'),
    choice(name: 'TAGVERTICAL', choices: ['ti'], description: 'Escolha da TAG VERTICAL/DEPARTAMENTO'),
    choice(name: 'TAGSERVICO', choices: ['eventbridge'], description: 'Escolha da TAG SERVICO'),
    choice(name: 'TAGCONTEXTO', choices: ['eventbridge'], description: 'Escolha da TAG CONTEXTO')
      ])
])

pipeline {
  agent any

  environment {
    BRANCH_NAME = 'main'
    GIT_REPO = ''  /#entrar com repositorio git#/
    GIT_CREDENTIALSID = '' /#entrar com suas credencias git#/
  }

  stages {

  stage('Confirmação PRD') {
  when {
    expression { params.ACCOUNT in ['PRD']}
  }
  steps {
    script {
      timeout(time: 3, unit: 'DAYS') {
        input(
          message: """⚠️ *Atenção!*
          Esta ação só pode ser executada pelo *time de *.
          Caso você não pertença a esse grupo, por favor *Entra em contato com o time de  para validação*.

          Deseja realizar o deployment em *Produção (PRD)*?""",
          submitter: 'admin',
          ok: 'Confirmar Deployment'
          ) 
        }
      }
    }
  }

    stage('Preparar Variáveis') {
      steps {
        script {
          env.TAG_CC_ID = sh(
            script: "echo '${params.TAGCC}' | sed 's/ -.*\$//'",
            returnStdout: true
          ).trim()

            echo "TAG_CC_ID extraído: ${env.TAG_CC_ID}"
        }
      }
    }

    stage('Configurar AWS Profile') {
        steps {
            script {
                def awsProfileMap = [
                    'DEV': '',
                    'HML': '',
                    'PRD': '',
                ]

              env.AWS_PROFILE = awsProfileMap[params.ACCOUNT]
                echo "Usando AWS_PROFILE=${env.AWS_PROFILE}"
            }
        }
    }

    stage('Validação de Parâmetros') {
            steps {
                script {
                    echo "Conta selecionada: ${params.ACCOUNT}"
                    echo "Nome da regra: ${params.NOME_RULE}"
                    echo "Descrição: ${params.Description}"
                    echo "Cron Expression: ${params.CRON_EXPRESSION}"
                    echo "Região: ${params.REGION}"
                    echo "TAGENV: ${params.TAGENV}"
                    echo "TAGCC: ${params.TAGCC}"
                    echo "TAGVERTICAL: ${params.TAGVERTICAL}"
                    echo "TAGSERVICO: ${params.TAGSERVICO}"
                    echo "TAGCONTEXTO: ${params.TAGCONTEXTO}"
                }
            }
        }

        stage('Criação da Regra EventBridge com múltiplos targets') {
            steps {
                script {
                    // Cria a regra principal (sempre)
                    sh """
                        aws events put-rule \
                            --name ${params.NOME_RULE} \
                            --schedule-expression "${params.CRON_EXPRESSION}" \
                            --state ${params.STATE_1} \
                            --description "${params.Description}" \
                            --region ${params.REGION} \
                            --tags Key=Name,Value=${params.NOME_RULE} \
                                   Key=Environment,Value=${params.TAGENV} \
                                   Key=CentroCusto,Value='${params.TAGCC}' \
                                   Key=Vertical,Value=${params.TAGVERTICAL} \
                                   Key=Servico,Value=${params.TAGSERVICO} \
                                   Key=Contexto,Value=${params.TAGCONTEXTO}
"""

            // Monta targets
            def targets = []
                  if (params.TARGET_ARN_1?.trim()) { targets << "{\"Id\":\"1\",\"Arn\":\"${params.TARGET_ARN_1}\"}" }
                  if (params.COUNT.toInteger() > 1 && params.TARGET_ARN_2?.trim()) { targets << "{\"Id\":\"2\",\"Arn\":\"${params.TARGET_ARN_2}\"}" }
                  if (params.COUNT.toInteger() > 2 && params.TARGET_ARN_3?.trim()) { targets << "{\"Id\":\"3\",\"Arn\":\"${params.TARGET_ARN_3}\"}" }

                  if (targets.size() > 0) {
            // Cria arquivo temporário com o JSON dos targets
                  writeFile file: 'targets.json', text: "[${targets.join(',')}]"

            // Adiciona todos os targets à regra do EventBridge
            sh "aws events put-targets --rule '${params.NOME_RULE}' --targets file://targets.json --region '${params.REGION}'"
            }

                }
            }

        post {
            success {
                echo "Regra(s) do EventBridge criada(s) com sucesso!"
            }
            failure {
                echo "Falha ao criar regra(s) do EventBridge!"
                        }
                  }
            }
      }
}
