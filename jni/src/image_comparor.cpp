/**
 * This cpp does following things:
 *  1. Convert the YUV image to MAT(of OpenCV) or other formats that are convenient for doing later motion detection. Because allocating image too frequently may lead to high java GC burden.
 *  2. Memory management of the converted images.
 */

#include <android/log.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>

static bool LOG_ENABLED = false;
#define  LOG_TAG    "image_comparor_cpp"
#define  LOGD(...)  if(LOG_ENABLED) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  if(LOG_ENABLED) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  if(LOG_ENABLED) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  if(LOG_ENABLED) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace cv;
/**
 * Declarations.
 */
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jint JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeConvertYUVImageToLocalImage(
    JNIEnv* env, jobject thiz, jbyteArray imageData, jint dataLen, jint imageWidth, jint imageHeight);

JNIEXPORT jboolean JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeReleaseLocalImage(
    JNIEnv* env, jobject thiz, jint imageHandle);

JNIEXPORT jintArray JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeDetectMotion(
    JNIEnv* env, jobject thiz, jint prePre, jint pre, jint current, jint sensitivity);

#ifdef __cplusplus
}
#endif

static int convertYUVDataToMat(void *data, int len, int width, int height);

/**
 * Definitions.
 */
JNIEXPORT jint JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeConvertYUVImageToLocalImage(
    JNIEnv* env, jobject thiz, jbyteArray imageData, jint dataLen, jint imageWidth, jint imageHeight) {
  int len = env->GetArrayLength (imageData);
  unsigned char* buf = new unsigned char[len];
  env->GetByteArrayRegion (imageData, 0, len, reinterpret_cast<jbyte*>(buf));
  return convertYUVDataToMat(reinterpret_cast<void*>(buf), len, imageWidth, imageHeight);
}

JNIEXPORT jboolean JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeReleaseLocalImage(
    JNIEnv* env, jobject thiz, jint imageHandle) {
  Mat *accordingMatPtr = reinterpret_cast<Mat*>(imageHandle);
  delete accordingMatPtr;
  return true;
}

static int* detectMotion(const Mat *const prePre, const Mat *const pre, const Mat *const current, int sensitivity);
static void rotateBy90(int *rect, int imageWidth, int imageHeight);
JNIEXPORT jintArray JNICALL Java_com_totrit_livemonitor_util_ImageComparor_nativeDetectMotion(
    JNIEnv* env, jobject thiz, jint prePre, jint pre, jint current, jint sensitivity) {
  Mat *f0 = reinterpret_cast<Mat*>(current);
  Mat *f1 = reinterpret_cast<Mat*>(pre);
  Mat *f2 = reinterpret_cast<Mat*>(prePre);
  if (!(f0->size() == f1->size() && f1->size() == f2->size())) {
    return NULL;
  }
  int *rect = detectMotion(f2, f1, f0, sensitivity);
  if (rect != NULL) {
//    rotateBy90(rect, base->cols, base->rows);
    jintArray result;
    result = env->NewIntArray(4);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }
    env->SetIntArrayRegion(result, 0, 4, rect);
    return result;
  } else {
    return NULL;
  }

}
//
//void rotateBy90(int *rect, int imageWidth, int imageHeight) {
//  int newLeft = imageHeight - rect[3]; // width - bottom
//  int newTop = rect[0]; // left
//  int newRight = imageHeight - rect[1]; // width - top
//  int newBottom = rect[2]; // right
//  rect[0] = newLeft;
//  rect[1] = newTop;
//  rect[2] = newRight;
//  rect[3] = newBottom;
//}

static int convertYUVDataToMat(void *data, int len, int width, int height) {
  Mat *newLocalImage = new Mat(height, width, CV_8UC1, data);
  if (newLocalImage == NULL) {
    LOGD("create native Mat failed.");
    return -1;
  } else {
    LOGD("new created Mat %p.", newLocalImage);
    return reinterpret_cast<int>(newLocalImage);
  }
}

/**
 * Given two images, get the motion rectangle.
 */
static int analizePixelsForChanges(const Mat &diff, int &left, int &top, int &right, int &bottom, int maxDeviation);
int* detectMotion(const Mat *const prePre, const Mat *const pre, const Mat *const current, int sensitivity) {
  //TODO
  int motionThreshold = 5;
  int maxDeviation = 60;
  int arg1ForThresholdFunc = 35;
  int arg2ForThresholdFunc = 255;
  int left = 0, top = 0, right = current->cols, bottom = current->rows;
  Mat kernel_ero = getStructuringElement(MORPH_RECT, Size(2,2));
  Mat diff1, diff2, diff;
  absdiff(*prePre, *current, diff1);
  absdiff(*pre, *current, diff2);
  bitwise_and(diff1, diff2, diff);
  threshold(diff, diff, arg1ForThresholdFunc, arg2ForThresholdFunc, CV_THRESH_BINARY);
  erode(diff, diff, kernel_ero);
//  imwrite("/sdcard/erode.png", diff);
  int numberOfChanges = analizePixelsForChanges(diff, left, top, right, bottom, maxDeviation);
  if (numberOfChanges > motionThreshold) {
    int *ret = new int[4];
    ret[0] = left; ret[1] = top; ret[2] = right; ret[3] = bottom;
    LOGD("motion detected, changes=%d, rect=(%d, %d, %d, %d)", numberOfChanges, left, top, right, bottom);
    return ret;
  } else {
    return NULL;
  }
}

int analizePixelsForChanges(const Mat &diff, int &left, int &top, int &right, int &bottom, int maxDeviation) {
  // calculate the standard deviation
  Scalar mean, stddev;
  meanStdDev(diff, mean, stddev);
  // if not to much changes then the motion is real (neglect agressive snow, temporary sunlight)
  LOGD("stddev[0] = %f", stddev[0]);
  if (stddev[0] < maxDeviation) {
    int number_of_changes = 0;
    int min_x = diff.cols, max_x = 0;
    int min_y = diff.rows, max_y = 0;
    // loop over image and detect changes
    for (int j = top; j < bottom; j += 2) {  // height
      for (int i = left; i < right; i += 2) {  // width
        // check if at pixel (j,i) intensity is equal to 255
        // this means that the pixel is different in the sequence
        // of images (prev_frame, current_frame, next_frame)
//        LOGD("intensity at(%d, %d) is %d", j, i, static_cast<int>(diff.at < uchar > (j, i)));
        if (static_cast<int>(diff.at < uchar > (j, i)) == 255) {
          number_of_changes++;
          if (min_x > i)
            min_x = i;
          if (max_x < i)
            max_x = i;
          if (min_y > j)
            min_y = j;
          if (max_y < j)
            max_y = j;
        }
      }
    }
    LOGD("number of changes: %d", number_of_changes);
    if (number_of_changes) {
      //check if not out of bounds
      if (min_x - 10 > 0)
        min_x -= 10;
      if (min_y - 10 > 0)
        min_y -= 10;
      if (max_x + 10 < diff.cols - 1)
        max_x += 10;
      if (max_y + 10 < diff.rows - 1)
        max_y += 10;
      left = min_x;
      top = min_y;
      right = max_x;
      bottom = max_y;
    }
    return number_of_changes;
  }
  return 0;
}
