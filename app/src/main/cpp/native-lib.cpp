
#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;
//#include <string>
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_learn_kotlin_com_androidopencv_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_jijigi_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_jijigi_MainActivity_detectEdgeJNI(JNIEnv *env, jobject thiz, jlong input_image,
                                                   jlong output_image, jint th1, jint th2) {
    // TODO: implement detectEdgeJNI()

    Mat &inputMat = *(Mat *) input_image;
    Mat &outputMat = *(Mat *) output_image;

    cvtColor(inputMat, outputMat, COLOR_RGB2GRAY);
    Canny(outputMat, outputMat, th1, th2);
}
