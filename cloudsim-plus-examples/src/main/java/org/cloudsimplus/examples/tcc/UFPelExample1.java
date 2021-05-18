/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples.tcc;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.network.topologies.BriteNetworkTopology;
import org.cloudbus.cloudsim.network.topologies.NetworkTopology;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.util.TimeUtil;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelPlanetLab;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class UFPelExample1 {
    private static final int HOSTS = 1;
    private static final int HOST_PES = 16;
    private static final int HOST_PES_MIPS = 10000;
    private static final int HOST_RAM = 65536; //in Megabytes
    private static final int HOST_BW = 10000; //in Megabits/s
    private static final int HOST_STORAGE = 1000000; //in Megabytes

    private static final int VMS = 1;
    private static final int VM_PES = 16;
    private static final int VM_PES_MIPS = 10000;
    private static final int VM_RAM = 65536; //in Megabytes
    private static final int VM_BW = 10000; //in Megabits/s
    private static final int VM_STORAGE = 1000000; //in Megabytes

    private static final int CLOUDLETS = 20745;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = 2600;

    private final CloudSim cloudSim;
    private DatacenterBroker broker;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter;

    public static void main(String[] args) {
        new UFPelExample1();
    }

    private UFPelExample1() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        final double startSecs = TimeUtil.currentTimeSecs();
        System.out.printf("Simulation started at %s%n%n", LocalTime.now());
        cloudSim = new CloudSim();
        datacenter = createDatacenter();
        broker = new DatacenterBrokerSimple(cloudSim);
        configureNetwork();

        vmList = createVms();
        cloudletList = createCloudlets();
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        cloudSim.start();

        final List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        System.out.printf("Simulation finished at %s. Execution time: %.2f seconds%n", LocalTime.now(), TimeUtil.elapsedSeconds(startSecs));

        System.out.println(getClass().getSimpleName() + " finished!");
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private Datacenter createDatacenter() {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }

        return new DatacenterSimple(cloudSim, hostList);
    }

    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        IntStream.range(0, HOST_PES).forEach(i -> peList.add(new PeSimple(HOST_PES_MIPS, new PeProvisionerSimple())));
        Host host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        return host;
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final Vm vm = new VmSimple(VM_PES_MIPS, VM_PES, new CloudletSchedulerSpaceShared());
            vm.setRam(VM_RAM);
            vm.setBw(VM_BW);
            vm.setSize(VM_STORAGE);
            list.add(vm);
        }

        return list;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 1% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.1);
        
        for (int i = 0; i < CLOUDLETS; i++) {
            final Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
            cloudlet.setSizes(1024);
            list.add(cloudlet);
        }

        return list;
    }

    /**
     * Create network.
     */
    private void configureNetwork() {
        //Configure network by mapping CloudSim entities to BRITE entities
        NetworkTopology networkTopology = new BriteNetworkTopology();
        cloudSim.setNetworkTopology(networkTopology);
        networkTopology.addLink(broker, datacenter, 10, 1);
        // networkTopology.addLink(datacenter, broker, 10.0, 1);
    }
}
