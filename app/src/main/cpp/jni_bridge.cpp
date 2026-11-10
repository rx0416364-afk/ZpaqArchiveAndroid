#include <jni.h>
#include <string>
#include <vector>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>

// zpaq.cpp is compiled with main=zpaq_main by CMake.
extern int zpaq_main(int argc, const char** argv);

extern "C" JNIEXPORT jint JNICALL
Java_com_example_zpaqarchive_ZpaqNative_runNative(
        JNIEnv* env,
        jobject /* thiz */,
        jobjectArray javaArgs,
        jstring javaWorkingDir,
        jstring javaLogPath) {

    if (javaArgs == nullptr || javaWorkingDir == nullptr || javaLogPath == nullptr) {
        return 90;
    }

    const char* cwdChars = env->GetStringUTFChars(javaWorkingDir, nullptr);
    const char* logChars = env->GetStringUTFChars(javaLogPath, nullptr);
    if (cwdChars == nullptr || logChars == nullptr) {
        if (cwdChars) env->ReleaseStringUTFChars(javaWorkingDir, cwdChars);
        if (logChars) env->ReleaseStringUTFChars(javaLogPath, logChars);
        return 91;
    }

    std::string workingDir(cwdChars);
    std::string logPath(logChars);
    env->ReleaseStringUTFChars(javaWorkingDir, cwdChars);
    env->ReleaseStringUTFChars(javaLogPath, logChars);

    const jsize count = env->GetArrayLength(javaArgs);
    std::vector<std::string> values;
    std::vector<const char*> argv;
    values.reserve(static_cast<size_t>(count));
    argv.reserve(static_cast<size_t>(count));

    for (jsize i = 0; i < count; ++i) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(javaArgs, i));
        if (item == nullptr) return 92;
        const char* chars = env->GetStringUTFChars(item, nullptr);
        if (chars == nullptr) {
            env->DeleteLocalRef(item);
            return 93;
        }
        values.emplace_back(chars);
        env->ReleaseStringUTFChars(item, chars);
        env->DeleteLocalRef(item);
    }
    for (const auto& value : values) argv.push_back(value.c_str());

    const int savedCwd = open(".", O_RDONLY);
    const int savedOut = dup(STDOUT_FILENO);
    const int savedErr = dup(STDERR_FILENO);
    if (savedCwd < 0 || savedOut < 0 || savedErr < 0) {
        if (savedCwd >= 0) close(savedCwd);
        if (savedOut >= 0) close(savedOut);
        if (savedErr >= 0) close(savedErr);
        return 94;
    }

    FILE* logFile = fopen(logPath.c_str(), "w");
    if (logFile == nullptr) {
        close(savedCwd); close(savedOut); close(savedErr);
        return 95;
    }

    fflush(stdout);
    fflush(stderr);
    const int logFd = fileno(logFile);
    dup2(logFd, STDOUT_FILENO);
    dup2(logFd, STDERR_FILENO);
    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    int result = 96;
    if (chdir(workingDir.c_str()) == 0) {
        try {
            result = zpaq_main(static_cast<int>(argv.size()), argv.data());
        } catch (...) {
            fprintf(stderr, "ZPAQ gagal karena kesalahan native yang tidak tertangani.\n");
            result = 97;
        }
    } else {
        fprintf(stderr, "Tidak dapat membuka direktori kerja aplikasi.\n");
    }

    fflush(stdout);
    fflush(stderr);
    fseek(logFile, 0, SEEK_END);
    fclose(logFile);

    dup2(savedOut, STDOUT_FILENO);
    dup2(savedErr, STDERR_FILENO);
    close(savedOut);
    close(savedErr);
    fchdir(savedCwd);
    close(savedCwd);

    return result;
}
