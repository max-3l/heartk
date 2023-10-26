# heartk

This programms implements ppg and eda signal features for kotlin and can be used in android applications.

## Dependencies

Add the packaged [ssj](https://mvnrepository.com/artifact/ca.umontreal.iro.simul/ssj) jar to the directory libs.

## Usage

Look at the code for the function definition. They are very simple to use. You just need to initialize the EDA or HRV objects and call `processFeatures`. For the HRV features you must extract ppg peaks first, using the PPG object.
