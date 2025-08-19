properties([
  parameters([
    choice(name: 'ACCOUNT', choices: ['DEV', 'HML', 'PRD'], description: 'Escolha a conta *'),
    choice(name: 'RDS_ACTION', choices: ['create', 'restore'], description: 'Criar nova instância ou restaurar de snapshot?'),
    string(name: 'SNAPSHOT_ID', defaultValue: '', description: 'Informe o ID do snapshot para restaurar (obrigatório se RDS_ACTION=restore)'),
    string(name: 'NOME', description: 'Nome da instância RDS'),
    choice(name: 'REGION', choices: ['us-east-1', 'sa-east-1'], description: 'Escolha a região *'),
    choice(name: 'ENGINE', choices: ['mysql', 'postgres', 'aurora'], description: 'Engine RDS'),
    choice(name: 'VOLUME_TYPE', defaultValue: 'gp3', choices: ['gp3', 'io1', 'io2'], description: 'Tipo do volume *'),
    choice(name: 'DISCO',     choices: ['30','40','50','60','80','100','200','300','400','500','1024',], ),
    choice(name: 'INSTANCE_CLASS', choices: ['db.t3.medium', 'db.t3.large', 'db.r5.large'], description: 'Classe da instância'),
    string(name: 'MASTER_USERNAME', description: 'Master User for RDS'),
    password(name: 'MASTER_PASSWORD', description: 'Master Passwords needs have minimum 16 characters, The same create automatic for default'),
    choice(name: 'MULTI_AZ', choices: ['false', 'true'],  description: 'Habilitar Multi-AZ (Alta Disponibilidade)?'),
    choice(  name: 'BACKUP_RETENTION',  defaultValue: '7', choices: ['1', '3', '7', '14', '30', '35'],  description: 'Período de retenção de backup (em dias)'),
    choice(name: 'CRIAR_ALARMES', choices: ['true', 'false'], description: 'Criar alarmes CloudWatch para o RDS EM PRD?'),

      [$class: 'CascadeChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      filterLength: 1,
      name: 'SUBNET',
      description: 'Subnets conforme conta',
      randomName: 'params_SUBNET',
      referencedParameters: 'ACCOUNT',
      script: [
       $class: 'GroovyScript',
        fallbackScript: [sandbox: true, script: 'return ["ERRO"]'],
        script: [sandbox: true, script: '''
          def account = ACCOUNT
          switch(account) {
            case "DEV":
              return [
                " SUBNETS PRIVADA  ENV - Region",
                "name vpc - Idvpc",
                "",
                " SUBNETS PRIVADA  ENV - Region",
                "name vpc - Idvpc,
                ]

            case "HML":
              return [
                " SUBNETS PRIVADA  ENV - Region",
                "name vpc - Idvpc",
                "",
                " SUBNETS PRIVADA  ENV - Region",
                "name vpc - Idvpc",
                 ]

            case "PRD":
              return [
                " SUBNETS PRIVADAS - ENV - Region ",
                "name vpc - Idvpc",
                "",
                " SUBNETS PRIVADAS - ENV - Region ",
                "name vpc - Idvpc",
                ]
            default:
              return ["Selecione uma conta válida"]
          }
        ''']
      ]
    ],

     [$class: 'CascadeChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      filterLength: 1,
      name: 'SECURITYGROUP',
      description: 'SecurityGroup conforme conta',
      randomName: 'params_SG',
      referencedParameters: 'ACCOUNT',
      script: [
        $class: 'GroovyScript',
        fallbackScript: [sandbox: true, script: 'return ["ERRO"]'],
        script: [sandbox: true, script: '''
          switch(ACCOUNT) {
            case "gr.DEV":
              return ["sg -identify id - Name sg (region)",
                      "sg -identify id - Name sg (region)"
                      ]
            case "gr.HML":
              return ["sg -identify id - Name sg (region)",
                      "sg -identify id - Name sg (region)"
                      ]
            case "gr.PRD":
              return [
                      "sg -identify id - Name sg (region)",
                      "sg -identify id - Name sg (region)"
                      ]
            default:
              return ["Selecione uma conta válida"]
          }
        ''']
      ]
    ],
         [  $class: 'CascadeChoiceParameter',
        choiceType: 'PT_SINGLE_SELECT',
        filterLength: 1,
        name: 'KMS_KEY',
        description: 'Chave KMS usada para habilitar Performance Insights',
        randomName: 'params_KMS_KEY',
        referencedParameters: 'ACCOUNT',
        script: [
        $class: 'GroovyScript',
        fallbackScript: [sandbox: true, script: 'return ["ERRO"]'],
        script: [sandbox: true, script: '''
switch(ACCOUNT) {
            case "gr.DEV":
              return ["data identify kms - name KMS (region)",
                      "data identify kms - name KMS (region)"
                      ]
            case "gr.HML":
              return ["data identify kms - name KMS (region)",
                      "data identify kms - name KMS (region)"
                      ]
            case "gr.PRD":
              return [
                      "data identify kms - name KMS (region)",
                      "data identify kms - name KMS (region)",
                      ]
            default:
              return ["Selecione uma conta válida"]
          }
        ''']
      ]
    ],
    
    
        [$class: 'CascadeChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      filterLength: 1,
      name: 'SERVICEBACKUP',
      description: 'Backup conforme região',
      randomName: 'params_SERVICEBACKUP',
      referencedParameters: 'REGION',
      script: [
        $class: 'GroovyScript',
        fallbackScript: [sandbox: true, script: 'return ["ERRO"]'],
        script: [sandbox: true, script: '''
          switch(REGION) {
            case "us-east-1":
              return ["Service_Backup_USE1"]
            case "sa-east-1":
              return ["Service_Backup_SAE1"]
            default:
              return ["Região inválida"]
          }
        ''']
      ]
    ],  
    choice(name: 'TAGENV', choices: ['dev','hml','prd','preprd','beta'], description: 'Tag Ambiente'),
    choice(name: 'TAGCC', choices: [
          '1111 - teste', ], description: 'Escolha da TAG DE CENTRO DE CUSTO'),
        choice(name: 'TAGBKP', 
        choices: ['RDS'], description: 'Escolha da TAG DE backup'),
        choice(name: 'TAGVERTICAL', 
        choices: ['ti'], description: 'Escolha da TAG VERTICAL/DEPARTAMENTO'),
        choice(name: 'TAGSERVICO', choices: ['aplicacao','iis','database','fileserver','firewall','splunk','splunk-gateway','plataforma','gateway'   ],     description: 'Escolha da TAG SERVICO'),
        choice(name: 'TAGCONTEXTO', choices: ['rds'],      description: 'Escolha da TAG CONTEXTO')
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
         /# Esta ação só pode ser executada pelo *time de *.
          Caso você não pertença a esse grupo, por favor *Entra em contato com o time de  para validação*.

          Deseja realizar o deployment em *Produção (PRD)*?""", #/ /# this massage u can edit for your language if want!! #/
          submitter: 'admin',
          ok: 'Confirmar Deployment'
          ) 
        }
      }
    }
  }

    stage('Git Checkout') {
      steps {
        script {
          sh "git log -1 --pretty=oneline"
          dir("${BUILD_NUMBER}/checkout") {
            git branch: env.BRANCH_NAME, credentialsId: env.GIT_CREDENTIALSID, url: env.GIT_REPO
          }
        }
      }
    }

    stage('Preparar Variáveis') {
      steps {
        script {
          env.TAG_CC_ID = sh(script: "echo '${params.TAGCC}' | sed 's/ -.*\$//'", returnStdout: true).trim()
          echo "TAG_CC_ID extraído: ${env.TAG_CC_ID}"
        }
      }
    }

    stage('Gerar senha') {
      steps {
        script {
          def generateRandomPassword = { int minLength ->
            def upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
            def lower = 'abcdefghijklmnopqrstuvwxyz'
            def digits = '0123456789'
            def symbols = '!#$%&*()+=-_' // caracteres permitidos
            def allChars = upper + lower + digits + symbols

            def random = new Random()
            def passwordChars = [
              upper[random.nextInt(upper.length())],
              lower[random.nextInt(lower.length())],
              digits[random.nextInt(digits.length())],
              symbols[random.nextInt(symbols.length())]
            ]

            (minLength - passwordChars.size()).times {
              passwordChars << allChars[random.nextInt(allChars.length())]
            }

            Collections.shuffle(passwordChars, random)
            return passwordChars.join('')
          }

          env.RDS_PASSWORD = generateRandomPassword(16)
          assert env.RDS_PASSWORD.length() >= 16 : "Senha gerada tem menos de 16 caracteres"
          echo 'Senha gerada com sucesso (mas não será exibida nos logs)'
        }
      }
    }
stage('Criar RDS') {
  steps {
script {
          def awsProfileMap = [
            'DEV': ',
            'HML': '',
            'PRD': '',

          ]

  // Torna acessível globalmente
  env.AWS_PROFILE = awsProfileMap[params.ACCOUNT]

        env.TAG_CC_ID = sh(
        script: "echo '${params.TAGCC}' | sed 's/ -.*\$//'",
        returnStdout: true
      ).trim()

      echo "TAG_CC_ID extraído: ${env.TAG_CC_ID}"
      
        def subnetId = sh(
        script: "echo '${params.SUBNET}' | sed 's/ -.*\$//'",
        returnStdout: true
        ).trim()

        echo "SUBNET extraído: ${subnetId}"

        def sgId = sh(
            script: "echo '${params.SECURITYGROUP}' | sed 's/ -.*\$//'",
                returnStdout: true
            ).trim()

        echo "SECURITYGROUP extraído: ${sgId}" 

        def KMSId = sh(
        script: "echo '${params.KMS_KEY}' | sed 's/ -.*\$//'",
        returnStdout: true
        ).trim()


        echo "KMS_KEY extraído: ${KMSId}" 

      def TAGBKP1 = "${params.TAGBKP}"
      def SERVICEBACKUP1 = "${params.SERVICEBACKUP}"

      def tagsJsonList = [
  [Key: "Name", Value: params.NOME],
  [Key: "ENV", Value: params.TAGENV],
  [Key: "CC", Value: env.TAG_CC_ID],
  [Key: "Vertical", Value: params.TAGVERTICAL],
  [Key: "Servico", Value: params.TAGSERVICO],
  [Key: "Contexto", Value: params.TAGCONTEXTO],
  [Key: "Backup", Value: TAGBKP1],
  [Key: "BackupService", Value: SERVICEBACKUP1]
    ]


      def multiAZFlag = params.MULTI_AZ.toBoolean() ? "--multi-az" : ""

      def tagsArgs = tagsJsonList.collect { tag -> "Key=${tag.Key},Value=${tag.Value}" }.join(' ')

      if (params.RDS_ACTION == 'restore') {
        if (!params.SNAPSHOT_ID?.trim()) {
          error("O parâmetro SNAPSHOT_ID deve ser preenchido para restaurar um snapshot.")
        }

        echo "Restaurando RDS a partir do snapshot: ${params.SNAPSHOT_ID}"
        sh """
          set +x
          export AWS_PROFILE=${AWS_PROFILE}

          aws rds restore-db-instance-from-db-snapshot \
            --db-instance-identifier ${params.NOME} \
            --db-snapshot-identifier ${params.SNAPSHOT_ID} \
            --db-instance-class ${params.INSTANCE_CLASS} \
            --vpc-security-group-ids ${sgId} \
            --db-subnet-group-name ${subnetId} \
            ${multiAZFlag} \
            --region ${params.REGION} \
            --profile ${AWS_PROFILE}
        """
      } else {
        echo "Criando nova instância RDS..."

        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[var: "RDS_PASSWORD"]]]) {
          sh """
            set +x
            export AWS_PROFILE=${AWS_PROFILE}
            export RDS_PASSWORD=$RDS_PASSWORD

            aws rds create-db-instance \
              --db-instance-identifier ${params.NOME} \
              --db-instance-class ${params.INSTANCE_CLASS} \
              --engine ${params.ENGINE} \
              --master-username ${params.MASTER_USERNAME} \
              --master-user-password \$RDS_PASSWORD \
              --storage-type ${params.VOLUME_TYPE} \
              --allocated-storage ${params.DISCO} \
              --vpc-security-group-ids ${sgId} \
              --db-subnet-group-name ${subnetId} \
              --enable-performance-insights \
              --performance-insights-kms-key-id ${KMSId} \
              --backup-retention-period ${params.BACKUP_RETENTION} \
              ${multiAZFlag} \
              --deletion-protection \
              --region ${params.REGION} \
              --profile ${AWS_PROFILE}
          """
        }
      }

      echo "Aguardando criação/restauração do RDS..."

      def rdsArn = sh(
        script: """
          aws rds describe-db-instances \
            --db-instance-identifier ${params.NOME} \
            --query "DBInstances[0].DBInstanceArn" \
            --output text \
            --region ${params.REGION} \
            --profile ${AWS_PROFILE}
        """,
        returnStdout: true
      ).trim()

      echo "Adicionando tags..."
      sh """
        aws rds add-tags-to-resource \
          --resource-name ${rdsArn} \
          --tags ${tagsArgs} \
          --region ${params.REGION} \
          --profile ${AWS_PROFILE}
      """
    }
  }
}
stage('Criar alarmes CloudWatch') {
  when {
    allOf {
      expression { params.ACCOUNT == 'PRD' }
      expression { params.CRIAR_ALARMES == 'true' }
    }
  }
  steps {
    script {
      def snsArn = "arn:aws:sns:us-east-1:account:namesns" // 🔁 Altere esse ARN se necessário

      def alarmConfigs = [
        [metric: "CPUUtilization",    threshold: 80,     comparison: "GreaterThanThreshold",  unit: "Percent",        eval: 2],
        [metric: "DBConnections",     threshold: 100,    comparison: "GreaterThanThreshold",  unit: "Count",          eval: 2],
        [metric: "FreeStorageSpace",  threshold: 5368709120, comparison: "LessThanThreshold", unit: "Bytes",          eval: 1], // 5 GB
        [metric: "ReadIOPS",          threshold: 100,   comparison: "GreaterThanThreshold",  unit: "Count/Second",   eval: 3],
        [metric: "WriteIOPS",         threshold: 100,   comparison: "GreaterThanThreshold",  unit: "Count/Second",   eval: 1]
      ]

      alarmConfigs.each { alarm ->
        def alarmName = "ALERTA CRITICO -NVI-${params.NOME}-${alarm.metric}"
        echo "Criando alarme: ${alarmName}"
        sh """
          aws cloudwatch put-metric-alarm \
            --alarm-name "${alarmName}" \
            --metric-name ${alarm.metric} \
            --namespace AWS/RDS \
            --statistic Average \
            --period 300 \
            --threshold ${alarm.threshold} \
            --comparison-operator ${alarm.comparison} \
            --evaluation-periods ${alarm.eval} \
            --dimensions Name=DBInstanceIdentifier,Value=${params.NOME} \
            --alarm-actions ${snsArn} \
            --ok-actions  ${snsArn} \
            --region ${params.REGION} \
            --profile ${env.AWS_PROFILE}
        """
      }
    }
  }
}

stage('Enviar e-mail com senha') {
post {
  success {
    script {
      def emailMap = [
        'DEV'  : ['@email.com'],
        'HML'  : ['@email.com'],
        'PRD'  : ['@email.com'],
      ]

      def recipients = emailMap.get(params.ACCOUNT, ['@email.com'])
      def joinedRecipients = recipients.collect { "\"${it}\"" }.join(",")

      def emailBody = """Successfully Job: ${env.BUILD_URL}

---------------------------

- Instância ${params.NOME} criada com sucesso!

- Favor:
  - Adicionar o novo servidor na planilha de controle EC2
  - Adicionar AntiVirus
  - Atualizar Windows Update
  - Instalar Client Telnet
  - Colocar no domínio, etc...
"""

      // Cria arquivo temporário com corpo
      writeFile file: 'emailBody.txt', text: emailBody

      // Prepara conteúdo com escape para uso no shell
      def bodyContent = readFile('emailBody.txt').replaceAll('"', '\\"')

      // Limpa o workspace do build após gerar o e-mail
      dir("${BUILD_NUMBER}") {
        cleanWs()
      }

      // Envia e-mail via AWS SES
      sh(script: """
        set +x
        aws ses send-email \\
          --from "jenkins@yourdomain.com" \\
          --destination "ToAddresses=[${joinedRecipients}]" \\
          --message Subject={Data=\\"Instância ${params.NOME} Criada com Sucesso: ${currentBuild.fullDisplayName}\\"},Body={Text={Data=\\"${bodyContent}\\"}} \\
          --region sa-east-1
        set -x
      """)
    }
  }

  failure {
    // Fallback simples em caso de falha (ainda via SES)
    script {
      sh(script: """
        set +x
        aws ses send-email \\
          --from "jenkins@yourdomain.com" \\
          --destination "ToAddresses=[\\"@email.com\\"]" \\
          --message Subject={Data=\\"Failed Pipeline: ${currentBuild.fullDisplayName}\\"},Body={Text={Data=\\"Failed Job: ${env.BUILD_URL}\\"}} \\
          --region sa-east-1
        set -x
      """)
    }
  }
}
}
