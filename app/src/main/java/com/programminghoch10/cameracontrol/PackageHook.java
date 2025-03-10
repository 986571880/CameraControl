package com.programminghoch10.cameracontrol;

import android.content.SharedPreferences;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;

public class PackageHook implements IXposedHookLoadPackage {
	
	private static XSharedPreferences getSharedPreferences() {
		XSharedPreferences sharedPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, "camera");
		if (!sharedPreferences.getFile().canRead()) sharedPreferences = null;
		if (sharedPreferences == null) {
			Log.e("CameraControl", "getSharedPreferences: failed to load SharedPreferences");
		}
		return sharedPreferences;
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) return;
		if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			OwnHook.hook(lpparam);
			return;
		}
		
		//Log.d("CameraControl", "handleLoadPackage: package="+lpparam.packageName);
		//XposedBridge.log("handleLoadPackage: package="+lpparam.packageName);
		
		XSharedPreferences sharedPreferences = getSharedPreferences();
		if (sharedPreferences == null) return;
		CameraPreferences cameraPreferences = new CameraPreferences(sharedPreferences);
		
		if (sharedPreferences.getBoolean("disableCameraManager", false)) {
			CameraManagerHook.disableHook(lpparam);
			CameraHook.disableHook(lpparam);
		} else {
			CameraManagerHook.hook(lpparam, cameraPreferences);
			CameraHook.hook(lpparam, cameraPreferences);
		}

		// 自用的一个修改，强制使用前置摄像头来扫码，有时间了也许写一个单独的模块，现在先用这个凑合着
		try{
			XposedHelpers.findAndHookMethod("com.google.zxing.client.android.camera.open.OpenCameraInterface",lpparam.classLoader,"open",int.class, new XC_MethodHook(){
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = 1;
				}
			});
		} catch(Throwable Throwable){}
	}
	
	public static class CameraPreferences {
		boolean disableFrontFacing = true;
		boolean disableBackFacing = true;
		boolean disableExternal = true;
		boolean blockList = true;
		boolean blockAccess = true;
		boolean blockFlash = true;
		
		CameraPreferences(SharedPreferences sharedPreferences) {
			disableFrontFacing = sharedPreferences.getBoolean("disableFrontFacing", true);
			disableBackFacing = sharedPreferences.getBoolean("disableBackFacing", true);
			disableExternal = sharedPreferences.getBoolean("disableExternal", true);
			blockList = sharedPreferences.getBoolean("blockList", true);
			blockAccess = sharedPreferences.getBoolean("blockAccess", true);
			blockFlash = sharedPreferences.getBoolean("blockFlash", true);
		}
		
		CameraPreferences() {
		}
		
		void setAll(boolean state) {
			disableFrontFacing = state;
			disableBackFacing = state;
			disableExternal = state;
			blockList = state;
			blockAccess = state;
			blockFlash = state;
		}
		
		void disableAll() {
			setAll(true);
		}
		
		void enableAll() {
			setAll(false);
		}
		
		boolean blockLegacy() {
			return blockList || blockAccess;
		}
	}
}
