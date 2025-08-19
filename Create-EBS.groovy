/******************
 ** CREATED BY LUAN COSTA **
 ** **
 ******************/

properties([
  parameters([
    choice(name: 'ACCOUNT', choices: ['DEV', 'HML', 'PRD'], description: 'Escolha a conta *'),
    string(name: 'NOME', description: 'Nome lógico do volume EBS *'),
    choice(name: 'REGION', choices: ['us-east-1', 'sa-east-1'], description: 'Região AWS *'),
    [$class: 'CascadeChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      filterLength: 1,
      name: 'AVAILABILITY_ZONE',
      description: 'Zona de disponibilidade conforme região',
      referencedParameters: 'REGION',
      script: [
        $class: 'GroovyScript',
        fallbackScript: [sandbox: true, script: 'return ["ERRO"]'],
        script: [sandbox: true, script: '''
          if (REGION == null) return ["Selecione uma região"]
          switch (REGION) {
            case "us-east-1":
              return [
                "us-east-1a", "us-east-1b", "us-east-1c",
                "us-east-1d", "us-east-1e", "us-east-1f"
              ]
            case "sa-east-1":
              return [
                "sa-east-1a", "sa-east-1b", "sa-east-1c",
                 "sa-east-1e", "sa-east-1f"
              ]
            default:
              return ["Nenhuma AZ disponível"]
          }
        ''']
      ]
    ],
    string(name: 'SIZE', defaultValue: '30', description: 'Tamanho do volume (GB) *'),
    choice(name: 'VOLUME_TYPE', choices: ['gp3', 'io1', 'io2', 'st1', 'sc1', 'standard'], description: 'Tipo do volume *'),
    booleanParam(name: 'ENCRYPTED', defaultValue: true, description: 'Criptografar volume?'),
    string(name: 'IOPS', defaultValue: '3000', description: 'IOPS (necessário para io1/io2)'),
    string(name: 'THROUGHPUT', defaultValue: '125', description: 'Throughput (MB/s maximum is 1000 - apenas gp3)'),
/*   string(name: 'INSTANCE_ID', defaultValue: '', description: 'ID da Instância EC2 (para auto-attach)'),*/
    choice(name: 'TAGENV', choices: ['dev','hml','prd','preprd','beta'], description: 'TAG DE AMBIENTE'),
    choice(name: 'TAGCC', choices: [
    '1111 - teste'
    ], description: 'Escolha da TAG DE CENTRO DE CUSTO'),
    choice(name: 'TAGVERTICAL', choices: ['ti'], description: 'TAG VERTICAL/DEPTO'),
    choice(name: 'TAGSERVICO', choices: ['ebs'], description: 'TAG SERVIÇO'),
    choice(name: 'TAGCONTEXTO', choices: ['ebs'], description: 'TAG CONTEXTO')
  ])
])

pipeline {
  agent any

  environment {
    BRANCH_NAME = 'main'
    GIT_REPO = ''
    GIT_CREDENTIALSID = ''
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
          Caso você não pertença a esse grupo, por favor *Entra em contato com o time de para validação*.

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
          echo "TAG_CC extraída: ${env.TAG_CC_ID}"
        }
      }
    }

    stage('Criar Volume EBS') {
      steps {
        script {
          def awsProfileMap = [
            'DEV': '',
            'HML': '',
            'PRD': '',

          ]

          def AWS_PROFILE = awsProfileMap[params.ACCOUNT]
          def ENVIRONMENT = params.ACCOUNT.replaceAll('gr.', '').toLowerCase()

          def volumeName = params.NOME
          def region = params.REGION
          def az = params.AVAILABILITY_ZONE
          def volumeSize = params.SIZE ?: '30'
          def volumeType = params.VOLUME_TYPE
          def iops = params.IOPS?.trim()
          def throughput = params.THROUGHPUT?.trim()
          def encrypted = params.ENCRYPTED ? '--encrypted' : ''

          def tagsJsonList = [
            [Key: "Name", Value: volumeName],
            [Key: "ENV", Value: params.TAGENV],
            [Key: "CC", Value: env.TAG_CC_ID],
            [Key: "Vertical", Value: params.TAGVERTICAL],
            [Key: "Servico", Value: params.TAGSERVICO],
            [Key: "Contexto", Value: params.TAGCONTEXTO]
          ]

          def tagSpec = groovy.json.JsonOutput.toJson([
            ResourceType: "volume",
            Tags: tagsJsonList
          ])

          def throughputParam = (volumeType == 'gp3' && throughput) ? "--throughput ${throughput}" : ""
          def iopsParam = ((volumeType == 'io1' || volumeType == 'io2') && iops) ? "--iops ${iops}" : ""

          echo "Usando AWS_PROFILE: ${AWS_PROFILE}"

          sh """
            export AWS_PROFILE=${AWS_PROFILE}
            aws ec2 create-volume \
              --region ${region} \
              --availability-zone ${az} \
              --size ${volumeSize} \
              --volume-type ${volumeType} \
              ${encrypted} \
              ${iopsParam} \
              ${throughputParam} \
              --tag-specifications '${tagSpec}'
          """
        }
      }
    }
  }
}
