/*
 * Loading/unloading and bookkeeping of device instances
 *
 * AUTHOR:
 * Nenad Amodaj, nenad@amodaj.com, 08/10/2005
 *
 * Copyright (c) 2006-2014 Regents of the University of California
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#pragma once

#include "../MMDevice/MMDevice.h"
#include "../MMDevice/DeviceThreads.h"
#include "CoreUtils.h"
#include "Devices/DeviceInstance.h"
#include "Error.h"
#include "Logging/Logger.h"

#include <boost/shared_ptr.hpp>
#include <boost/weak_ptr.hpp>

#include <map>
#include <string>
#include <vector>

class CMMCore;
class HubInstance;
class LoadedDeviceAdapter;


namespace mm
{

class DeviceManager /* final */
{
   // Store devices in an ordered container. We could use a map or hash map to
   // retrieve by name, but the number of devices is so small that it is not
   // known to be worth it.
   std::vector< std::pair<std::string, boost::shared_ptr<DeviceInstance> > > devices_;
   typedef std::vector< std::pair<std::string, boost::shared_ptr<DeviceInstance> > >::const_iterator
      DeviceConstIterator;
   typedef std::vector< std::pair<std::string, boost::shared_ptr<DeviceInstance> > >::iterator
      DeviceIterator;

   // Map raw device pointers to DeviceInstance objects, for those few places
   // where we need to retrieve device information from raw pointers.
   std::map< const MM::Device*, boost::weak_ptr<DeviceInstance> > deviceRawPtrIndex_;

public:
   ~DeviceManager();

   /**
    * \brief Load the specified device and assign a device label.
    */
   boost::shared_ptr<DeviceInstance>
   LoadDevice(boost::shared_ptr<LoadedDeviceAdapter> module,
         const std::string& deviceName, const std::string& label, CMMCore* core,
         mm::logging::Logger deviceLogger,
         mm::logging::Logger coreLogger);

   /**
    * \brief Unload a device.
    */
   void UnloadDevice(boost::shared_ptr<DeviceInstance> device);

   /**
    * \brief Unload all devices.
    */
   void UnloadAllDevices();

   /**
    * \brief Get a device by label.
    */
   ///@{
   boost::shared_ptr<DeviceInstance> GetDevice(const std::string& label) const;
   boost::shared_ptr<DeviceInstance> GetDevice(const char* label) const;
   ///@}

   /**
    * \brief Get a device by label, requring a specific type.
    */
   ///@{
   template <class TDeviceInstance>
   boost::shared_ptr<TDeviceInstance>
   GetDeviceOfType(boost::shared_ptr<DeviceInstance> device) const
   {
      if (device->GetType() != TDeviceInstance::RawDeviceClass::Type)
         throw CMMError("Device " + ToQuotedString(device->GetLabel()) +
               " is of the wrong type for the requested operation");
      return boost::static_pointer_cast<TDeviceInstance>(device);
   }

   template <class TDeviceInstance>
   boost::shared_ptr<TDeviceInstance> GetDeviceOfType(const std::string& label) const
   { return GetDeviceOfType<TDeviceInstance>(GetDevice(label)); }

   template <class TDeviceInstance>
   boost::shared_ptr<TDeviceInstance> GetDeviceOfType(const char* label) const
   { return GetDeviceOfType<TDeviceInstance>(GetDevice(label)); }
   ///@}

   /**
    * \brief Get a device from a raw pointer to its MMDevice object.
    */
   boost::shared_ptr<DeviceInstance> GetDevice(const MM::Device* rawPtr) const;

   /**
    * \brief Get the labels of all loaded devices of a given type.
    */
   std::vector<std::string> GetDeviceList(MM::DeviceType t = MM::AnyType) const;

   /**
    * \brief Get the labels of all loaded peripherals of a hub device.
    */
   std::vector<std::string> GetLoadedPeripherals(const char* hubLabel) const;
   // TODO GetLoadedPeripherals() should be a HubInstance method.

   /**
    * \brief Get the parent hub device of a peripheral.
    *
    * \param device
    * \return The hub device, or null if device has no parent.
    */
   boost::shared_ptr<HubInstance> GetParentDevice(boost::shared_ptr<DeviceInstance> device) const;
   // TODO GetParentDevice() should be a DeviceInstance method.
};


// Scoped acquisition of a device's module's lock
class DeviceModuleLockGuard
{
   MMThreadGuard g_;
public:
   explicit DeviceModuleLockGuard(boost::shared_ptr<DeviceInstance> device);
};

} // namespace mm
