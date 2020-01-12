def getRepoURL() {
    sh "git config --get remote.origin.url > .git/remote-url"
    return readFile(".git/remote-url").trim()
}

def setBuildStatus(String message, String state) {
    repoUrl = getRepoURL()
    step([
        $class: "GitHubCommitStatusSetter",
        reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
        statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ],
        errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]]
    ])
}

pipeline {
    agent any
    stages {
        stage('Git') {
            steps {
                // Get some code from a GitHub repository
                git credentialsId: '95234cec-ed53-43f2-b93e-c2908c564971', url: 'https://github.com/GoryMoon/CandyFix'
                setBuildStatus("Build " + currentBuild.displayName + " in progress...", "PENDING")
            }
        }
        stage('Setup') {
            steps {
                // Run the build
                sh './gradlew clean --continue --no-daemon'
                sh './gradlew setupCiWorkspace --continue --no-daemon'
            }
        }
        stage('Build') {
            steps {
                withCredentials([string(credentialsId: 'c0829fae-f3f1-4c2f-9609-6dd535f3ec73', variable: 'key_loc'), string(credentialsId: '482d51af-6be9-4df0-952a-ffb32438a296', variable: 'key_pass'), string(credentialsId: '5637b72a-1312-422b-a3a9-aed08440a90a', variable: 'local_maven')]) {
                    // Run the build
                    sh './gradlew build -Plocal_maven=${local_maven} -Pkeystore_location=${key_loc} -Pkeystore_password=${key_pass} --continue --no-daemon'
                }
            }
        }
        stage('Upload') {
            steps {
                withCredentials([string(credentialsId: 'c0829fae-f3f1-4c2f-9609-6dd535f3ec73', variable: 'key_loc'), string(credentialsId: '482d51af-6be9-4df0-952a-ffb32438a296', variable: 'key_pass'), string(credentialsId: '5637b72a-1312-422b-a3a9-aed08440a90a', variable: 'local_maven')]) {
                    sh './gradlew uploadJars -Plocal_maven=${local_maven} -Pkeystore_location=${key_loc} -Pkeystore_password=${key_pass} --no-daemon'

                    // Save and fingerprint
                    fingerprint 'build/libs/*.jar'
                    archiveArtifacts 'build/libs/*.jar'
                }
            }
        }
    }
    post {
        success {
            setBuildStatus("Build " + currentBuild.displayName + " succeeded", "SUCCESS")
        }
        unstable {
            setBuildStatus("Build " + currentBuild.displayName + " found unstable", "FAILURE")
        }
        failure {
            setBuildStatus("Build " + currentBuild.displayName + " failed", "ERROR")
        }
        aborted {
            setBuildStatus("Build " + currentBuild.displayName + " was aborted", "FAILURE")
        }
    }
}