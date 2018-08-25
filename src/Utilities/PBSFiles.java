package Utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PBSFiles {
	
	public static void main(String [] args) throws IOException{
		String nameOfJar = "DeceptionMILPBisectionEps.jar";
		
		int startConfigs = 100;
		int endConfigs = 100; 
		int startSystems = 50; 
		int endSystems = 50; 
		int startObs = 50;
		int endObs = 100;
		int interval = 10;
		int totalInstances = 30;
		int experimentNum = 110;
		double milpGap = 0;
		double epsilon = .05;
		int timeCutoff = 14390;
		int intervalInt = 6;
		
//		printMILPBisectionFiles(nameOfJar, startConfigs, endConfigs, startSystems, endSystems, startObs, endObs, interval, totalInstances, experimentNum, milpGap, timeCutoff, intervalInt);
		
//		printMILPFiles(nameOfJar, startConfigs, endConfigs, startSystems, endSystems, startObs, endObs, interval, totalInstances, experimentNum, milpGap, timeCutoff, intervalInt);
//
//		printMILPCFiles(nameOfJar, numConfigs, numSystems, numOfObs, interval, 1, totalInstances, experimentNum);

//		printDeceptionMILPExperimentFiles(nameOfJar, startConfigs, endConfigs, startSystems, endSystems, startObs, endObs, interval, 
//				totalInstances, experimentNum, milpGap, timeCutoff, intervalInt);
		
		printDeceptionMILPBisectionExperimentFiles(nameOfJar, startConfigs, endConfigs, startSystems, endSystems, startObs, endObs, interval, 
				totalInstances, experimentNum, milpGap, timeCutoff, intervalInt, epsilon);
		
		printMILPBisectionFiles(nameOfJar, startConfigs, endConfigs, startSystems, endSystems, startObs, endObs, interval, 
				totalInstances, experimentNum, milpGap, timeCutoff, intervalInt, epsilon);
		
		
//		printDeceptionMILPCExperimentFiles(nameOfJar, numConfigs, numSystems, numOfObs, interval, 3, totalInstances, experimentNum);
		
//		printGMMFiles(nameOfJar, startConfigs, startSystems, startObs, interval, totalInstances, experimentNum, true, .00);
		
//		printGMMFiles();
		
	}
	
	public static void printDeceptionMILPCExperimentFiles(String nameOfJar, int startConfigs, int endConfigs, int startSystems, int endSystems, 
			int startObs, int endObs, int interval, int numCuts, int totalInstances, int experimentNum) throws IOException{
		String output = "";
		
		int index = 1;
		
		for(int con=startConfigs; con<=endConfigs; con+=interval){
			for(int sys=startSystems; sys<=endSystems; sys+=interval){
				for(int obs=startObs; obs<=endObs; obs+=interval){
					output = "RunDeceptionMILPC"+numCuts+"Experiments"+obs+"_"+index+".pbs";
					PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
					
					w.print("#!/bin/bash\n");
					w.print("#PBS -l nodes=1:ppn=1\n");
					w.print("#PBS -l walltime=00:1:00\n");
					w.print("cd /home/rcf-40/aschlenk/Deception/PBS\n");
					w.print("\n");
					
					w.print("OBS="+obs+"\n");
					w.print("\n");
		
					w.print("for NUMBERS in 1 7 13 19 25 ; do\n");
					w.print("    qsub DeceptionMILPC"+numCuts+"_"+sys+"_"+obs+"_${NUMBERS}_"+experimentNum+".pbs\n");
					w.print("    echo \"Submitting ... \" ${OBS} ${START} ${END}\n");
					w.print("done\n");
					
								
					w.close();
					index++;
				}
			}
		}
	}
	
	public static void printDeceptionMILPBisectionExperimentFiles(String nameOfJar, int startConfigs, int endConfigs, int startSystems, int endSystems, int startObs, int endObs, 
			int interval, int totalInstances, int experimentNum, double milpGap, int timeCutoff, int instanceInt, double epsilon) throws IOException{
		String output = "";
		
		int index = 1;
		
		for(int con=startConfigs; con<=endConfigs; con+=interval){
			for(int sys=startSystems; sys<=endSystems; sys+=interval){
				for(int obs=startObs; obs<=endObs; obs+=interval){
					output = "RunDeceptionMILPBisectionExperiments_"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_"+index+"_"+experimentNum+ "_" + milpGap + "_" + epsilon +".pbs";
			
					PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
			
					w.print("#!/bin/bash\n");
					w.print("#PBS -l nodes=1:ppn=1\n");
					w.print("#PBS -l walltime=00:01:00\n");
					w.print("cd /home/rcf-40/aschlenk/Deception/PBS\n");
					w.print("\n");
					
					w.print("\n");
					
					//"DeceptionMILPBisection"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_"+instance+"_"+experimentNum+"_"+milpGap+"_" + epsilon +".pbs";
		
					w.print("for NUMBERS in 1 7 13 19 25 ; do\n");
					w.print("    sbatch DeceptionMILPBisection"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_${NUMBERS}_"+experimentNum+"_"+ milpGap + "_" + epsilon +".pbs\n");
					w.print("    echo \"Submitting ... \" ${OBS} ${START} ${END}\n");
					w.print("done\n");
					
								
					w.close();
					index++;
				}	
			}
		}
	}
	
	public static void printDeceptionMILPExperimentFiles(String nameOfJar, int startConfigs, int endConfigs, int startSystems, int endSystems, int startObs, int endObs, 
			int interval, int totalInstances, int experimentNum, double milpGap, int timeCutoff, int instanceInt) throws IOException{
		String output = "";
		
		int index = 1;
		
		for(int con=startConfigs; con<=endConfigs; con+=interval){
			for(int sys=startSystems; sys<=endSystems; sys+=interval){
				for(int obs=startObs; obs<=endObs; obs+=interval){
					output = "RunDeceptionMILPExperiments_"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_"+index+"_"+experimentNum+".pbs";
			
					PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
			
					w.print("#!/bin/bash\n");
					w.print("#PBS -l nodes=1:ppn=1\n");
					w.print("#PBS -l walltime=00:01:00\n");
					w.print("cd /home/rcf-40/aschlenk/Deception/PBS\n");
					w.print("\n");
					
//					w.print("SYS="+sys+"\n");
					w.print("\n");
		
					w.print("for NUMBERS in 1 7 13 19 25 ; do\n");
					w.print("    qsub DeceptionMILP"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_${NUMBERS}_"+experimentNum+".pbs\n");
					w.print("    echo \"Submitting ... \" ${OBS} ${START} ${END}\n");
					w.print("done\n");
					
								
					w.close();
					index++;
				}	
			}
		}
	}
	
	private static void printGMMFiles(String nameOfJar, int numConfigs, int numSystems, int numOfObs, int intervalObs, int totalInstances, 
			int experimentNum, boolean hardGMM, double lambda) throws IOException {
		String output = "";
		System.out.println(numSystems);
//		for (int obs = numOfObs; obs <= 100; obs += intervalObs) {
		for(int sys = numSystems; sys <= 20; sys+=intervalObs){
			output = "DeceptionGMM_" + hardGMM + "_" + lambda + "_" + sys + "_" + numOfObs + "_" + experimentNum +".pbs";
			System.out.println(output);

			PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

			w.print("#!/bin/bash\n");
			w.print("#PBS -l nodes=1:ppn=2\n");// :sl250s\n");
			w.print("#PBS -l walltime=1:00:00\n");
			w.print("cd /home/rcf-40/aschlenk/Deception\n");
			
			w.print("\n");
			w.print("for NUMBERS in 1000 2000 ; do\n");
			w.print("	java -jar " + nameOfJar + " " + experimentNum + " " + numConfigs + " " + sys + " " + numOfObs
					+ " " + 1 + " " + totalInstances + " " + 1800 + " " + false + " " + false + " " + 1
					+ " " + true + " " + hardGMM + " " + "${NUMBERS}"  + " " + lambda + "\n");
			w.print("done\n");
			
			w.close();
		}
	}
	
	private static void printMILPCFiles(String nameOfJar, int numConfigs, int numSystems, int numOfObs, int intervalObs, int numCuts, 
			int totalInstances, int experimentNum) throws IOException {
		String output = "";

		for (int instance = 1; instance <= totalInstances; instance += 15) {
			for (int obs = numOfObs; obs <= 100; obs += intervalObs) {
				output = "DeceptionMILPC"+numCuts+"_"+numSystems+"_"+obs+"_"+instance+"_"+ experimentNum + ".pbs";
				System.out.println(output);

				PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

				w.print("#!/bin/bash\n");
				w.print("#PBS -l nodes=1:ppn=2\n");// :sl250s\n");
				w.print("#PBS -l walltime=8:00:00\n");
				w.print("cd /home/rcf-40/aschlenk/Deception\n");
				w.print("java -jar " + nameOfJar + " " + experimentNum + " " + numConfigs + " " + numSystems + " " + obs
						+ " " + instance + " " + (instance + 14) + " " + 1800 + " " + false + " " + true + " " + numCuts + " "
						+ false + " " + false + " " + 1000 + " " + .1 + "\n");

				w.close();
			}
		}
	}

	public static void printMILPFiles(String nameOfJar, int startConfigs, int endConfigs, int startSystems, int endSystems, int startObs, int endObs, 
			int interval, int totalInstances, int experimentNum, double milpGap, int timeCutoff, int instanceInt) throws IOException{
		//String nameOfJar = "JAIRFullExperiment.jar";
		
		String output="";
		
		for(int instance=1; instance<=totalInstances; instance+=instanceInt){
			for(int con=startConfigs; con<=endConfigs; con+=interval){
				for(int sys=startSystems; sys<=endSystems; sys+=interval){
					for(int obs=startObs; obs<=endObs; obs+=interval){
						output = "DeceptionMILP"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_"+instance+"_"+experimentNum+".pbs";
						System.out.println(output);
							
						PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
			
						w.print("#!/bin/bash\n");
						w.print("#PBS -l nodes=1:ppn=4\n");// :sl250s\n");
						w.print("#PBS -l walltime=24:00:00\n");
						w.print("cd /home/rcf-40/aschlenk/Deception\n");
						w.print("java -jar " + nameOfJar + " " + experimentNum + " " + con +" "+ sys + " " + obs + " " +instance + " " +
								(instance+instanceInt-1) + " " + timeCutoff + " " + true + " " + false + " " + 1 + " " + milpGap + "\n");
			
						w.close();
					}
				}
			}
		}

	}
	
	public static void printMILPBisectionFiles(String nameOfJar, int startConfigs, int endConfigs, int startSystems, int endSystems, int startObs, int endObs, 
			int interval, int totalInstances, int experimentNum, double milpGap, int timeCutoff, int instanceInt, double epsilon) throws IOException{
		//String nameOfJar = "JAIRFullExperiment.jar";
		
		String output="";
		
		for(int instance=1; instance<=totalInstances; instance+=instanceInt){
			for(int con=startConfigs; con<=endConfigs; con+=interval){
				for(int sys=startSystems; sys<=endSystems; sys+=interval){
					for(int obs=startObs; obs<=endObs; obs+=interval){
						output = "DeceptionMILPBisection"+timeCutoff+"_"+con+"_"+sys+"_"+obs+"_"+instance+"_"+experimentNum+"_"+milpGap+"_" + epsilon +".pbs";
						System.out.println(output);
							
						PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
			
						w.print("#!/bin/bash\n");
						w.print("#PBS -l nodes=1:ppn=4\n");// :sl250s\n");
						w.print("#PBS -l walltime=24:00:00\n");
						w.print("cd /home/rcf-40/aschlenk/Deception\n");
						w.print("java -jar " + nameOfJar + " " + experimentNum + " " + con +" "+ sys + " " + obs + " " +instance + " " +
								(instance+instanceInt-1) + " " + timeCutoff + " " + true + " " + milpGap + " " +epsilon+ "\n");
			
						w.close();
					}
				}
			}
		}

	}
	
	
}
