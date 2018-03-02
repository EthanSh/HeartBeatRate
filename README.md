# HeartBeatRate

I use low pass filter to filter the input data
Then calculate the mean of the camera frame as feature value.
To detect whether the finger is fully touched, I check the four corners of the frame if they are the same.

Then I use Fourier transform to get the significant fraquency, regarding it as the heartbeat rate.
To do that I collect 128 datas as one window frame. 
