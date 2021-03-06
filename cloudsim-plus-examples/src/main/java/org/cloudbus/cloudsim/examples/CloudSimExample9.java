/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSimple;
import org.cloudbus.cloudsim.schedulers.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostDynamicWorkloadSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSimple;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.CloudletsTableBuilderHelper;
import org.cloudbus.cloudsim.util.TextTableBuilder;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.resources.FileStorage;
import org.cloudbus.cloudsim.resources.Ram;

/**
 * A simple example showing how to create a datacenter with two hosts,
 * with one Vm in each one, and run 1 cloudlet in each Vm. 
 * At the end, it shows the total resource utilization of hosts 
 * into a datacenter (considering the usage of their VMs).
 *
 * Cloudlets run in VMs with different MIPS requirements. The cloudlets will
 * take different time to complete the execution depending on the requested VM
 * performance.
 * 
 * Code originated from the {@link CloudSimExample3} class.<br/>
 * 
 * @author <a href="http://manoelcampos.com">Manoel Campos da Silva Filho</a>
 */
public class CloudSimExample9 {

    /**
     * The cloudlet list.
     */
    private static List<Cloudlet> cloudletList;

    /**
     * The vmlist.
     */
    private static List<Vm> vmlist;

    /**
     * Creates main() to run this example
     * @param args
     */
    public static void main(String[] args) {
        Log.printFormattedLine("Starting %s...", CloudSimExample9.class.getSimpleName());

        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            //Third step: Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            //Fourth step: Create one virtual machine
            vmlist = new ArrayList<>();

            //VM description
            int vmid = -1;
            int mips = 1000;
            long size = 10000; //image size (MB)
            int ram = 2048; //vm memory (MB)
            long bw = 1000;
            int pesNumber = 1; //number of cpus
            String vmm = "Xen"; //VMM name

            //create two VMs
            Vm vm1 = new VmSimple(++vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

            //the second VM will have twice the priority of VM1 and so will receive twice CPU time
            Vm vm2 = new VmSimple(++vmid, brokerId, mips * 2, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

            //add the VMs to the vmList
            vmlist.add(vm1);
            vmlist.add(vm2);

            //submit vm list to the broker
            broker.submitVmList(vmlist);

            //Fifth step: Create two Cloudlets
            cloudletList = new ArrayList<>();

            //Cloudlet properties
            int id = -1;
            long length = 10000;
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            Cloudlet cloudlet1 = 
                    new CloudletSimple(++id, length, pesNumber, fileSize, outputSize, 
                            utilizationModel, utilizationModel, utilizationModel);
            cloudlet1.setUserId(brokerId);

            Cloudlet cloudlet2 = 
                    new CloudletSimple(++id, length, pesNumber, fileSize, outputSize, 
                            utilizationModel, utilizationModel, utilizationModel);
            cloudlet2.setUserId(brokerId);

            //add the cloudlets to the list
            cloudletList.add(cloudlet1);
            cloudletList.add(cloudlet2);

            //submit cloudlet list to the broker
            broker.submitCloudletList(cloudletList);

            //bind the cloudlets to the vms. This way, the broker
            // will submit the bound cloudlets only to the specific VM
            broker.bindCloudletToVm(cloudlet1.getId(), vm1.getId());
            broker.bindCloudletToVm(cloudlet2.getId(), vm2.getId());

            // Sixth step: Starts the simulation
            final double finishTime = CloudSim.startSimulation();

            CloudSim.stopSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletsFinishedList();

            showCpuUtilizationForAllHosts(finishTime, datacenter0);

            String title = (newList.isEmpty() ? "Finished cloudlet list is empty" : "Executed cloudlets");
            CloudletsTableBuilderHelper.print(new TextTableBuilder(title), newList);
            Log.printFormattedLine("%s finished!", CloudSimExample9.class.getSimpleName());
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    /**
     * Shows CPU utilization of all hosts into a given datacenter.
     * 
     * @param simulationFinishTime The time the simulation has finished
     * @param datacenter0 There datacenter where to check the utilization of hosts
     * 
     * @todo It has to be checked if the results are correct. The results 
     * are suspicious.
     */
    private static void showCpuUtilizationForAllHosts(final double simulationFinishTime, Datacenter datacenter0) {
        Log.printLine("\nHosts CPU utilization history for the entire simulation period");
        Log.printConcatLine("Simulation finish time: ", simulationFinishTime);
        int numberOfUsageHistoryEntries = 0;
        final double interval = 1;
        for (Host host : datacenter0.getHostList()) {
            for(int clock = 0; clock <= simulationFinishTime; clock+=interval){
                final double hostCpuUsage
                        = getHostTotalUtilizationOfCpuInMips(host, simulationFinishTime);
                if(hostCpuUsage > 0){
                    numberOfUsageHistoryEntries++;
                    Log.printConcatLine(
                            " Time: ", clock,
                            "\tHost: ", host.getId(),
                            "\t\tCPU Utilization (MIPS): ", hostCpuUsage);
                }
            }
            Log.printLine("--------------------------------------------------");
        }
        if(numberOfUsageHistoryEntries == 0)
            Log.printLine(" No CPU usage history was found");
    }

    /**
     * Gets the total CPU utilization of host at a given time.
     * 
     * @param host
     * @param time
     * @return The total host CPU utilization in MIPS for the requested time
     */
    public static double getHostTotalUtilizationOfCpuInMips(Host host, double time) {
        double totalHostUtilization = 0;
        for (Vm vm : host.getDatacenter().getVmList()) {
            if(vm.getHost().equals(host)){
                totalHostUtilization += vm.getTotalUtilizationOfCpuMips(time);
            }
        }

        return totalHostUtilization;
    }

    private static Datacenter createDatacenter(String name) {
        // Here are the steps needed to create a DatacenterSimple:
        // 1. We need to create a list to store our machine
        List<Host> hostList = new ArrayList<>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList1 = new ArrayList<>();

        int mips = 1200;

        // 3. Create PEs and add these into a list.
        peList1.add(new PeSimple(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        //4. Create Hosts with its id and list of PEs and add them to the list of machines
        int hostId = -1;
        int ram = 2048; //host memory (MB)
        long storage = 1000000; //host storage
        long bw = 10000;

        hostList.add(new HostDynamicWorkloadSimple(
                        ++hostId,
                        new ResourceProvisionerSimple(new Ram(ram)),
                        new ResourceProvisionerSimple(new Bandwidth(bw)),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        ); // This is our first machine

        //create another machine in the Data center
        List<Pe> peList2 = new ArrayList<>();
        peList2.add(new PeSimple(0, new PeProvisionerSimple(mips * 2)));

        hostList.add(new HostDynamicWorkloadSimple(
                        ++hostId,
                        new ResourceProvisionerSimple(new Ram(ram)),
                        new ResourceProvisionerSimple(new Bandwidth(bw)),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        ); // This is our second machine

		// 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.001;	// the cost of using storage in this resource
        double costPerBw = 0.0;			// the cost of using bw in this resource
        LinkedList<FileStorage> storageList = new LinkedList<>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple (
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a DatacenterSimple object.
        DatacenterSimple datacenter = null;
        try {
            datacenter = new DatacenterSimple(
                    name, characteristics, 
                    new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
    //to the specific rules of the simulated scenario
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBrokerSimple("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }
}
