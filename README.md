# Deception_Algorithms


This library contains the code used in the AAMAS '18 paper "Deceiving cyber adversaries: A game theoretic approach".  


## Using the Library

The library can be used to run single game instances which can be randomly generated or given as input to the program in the form of a text file. 


## Solving a Cyber Deception Game

The Cyber Deception Game takes as input a representation of an enterprise network, the set of deceptive states that can be used to mask the true configuration of a computer, and other potential cost/service constraints on the use of the deceptive network configuration. 

To generate a random game instance, create a new instance of DeceptionGame and call .generateGame(...) with the proper arguments for the game size.  The game can also be set manually by passing in the parameters for the game instance. 


##### Using the optimization algorithms to solve a Cyber Deception Game


First, the cplex library needs to be loaded before running an optimization algorithm to solve for the prescribed deceptive policy.  After Cplex is loaded, the library src/solvers/GameSolver.java can be called to run the regular Mixed-integer Linear Program to solve for the optimal policy with various parameters that can be set as described in the documentation.  The Greedy Min-Max solver used as the heuristic algorithm can be run by using src/solvers/GreedyMinMaxSolver.java.  

To run the algorithms to solve for the optimal policy against a naive adversary, the library src/solvers/NaiveSolver.java can be used.  This library has two versions of the solver.  One solves for the optimal policy without cost and the other solves for it with cost. 



##### Optimization Solvers included in Chapter Version of Paper

There is also the Mixed-integer Linear Programming bisection algorithm which is included in this optimization library.  This includes a version of the bisection algorithm which solves a LP at each step of the algorithm instead of a MILP.  The algorithms can be accessed by calling the Bisection src/solvers/BisectionMILP.java and src/solvers/Bisection.java. 









