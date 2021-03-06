/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

#include "mds-debug.h"
#include "mds_core_api.h"                           // MDS Core API
#include "mds_jni.h"

using namespace mds;
using namespace mds::api;
using namespace mds::jni;

namespace mds
{
  namespace jni
  {
    namespace nanaged_type
    {

      template<kind KIND>
	inline api_type<KIND>
	lookup_intern (api_type<kind::LONG> ctxtHIndex,
		       api_type<kind::LONG> nsHIndex,
		       api_type<kind::LONG> nameHIndex)
	{
	  indexed<iso_context_handle> ctxt
	    { ctxtHIndex };
	  indexed<namespace_handle> ns
	    { nsHIndex };
	  indexed<interned_string_handle> name
	    { nameHIndex };
	  return ns->lookup (*ctxt, *name, managed_handle_by_kind<KIND> ());
	}

      template<kind KIND>
	inline typename str_to_long<KIND>::type
	lookup (api_type<kind::LONG> ctxtHIndex, api_type<kind::LONG> nsHIndex,
		api_type<kind::LONG> nameHIndex)
	{
	  return lookup_intern<KIND> (ctxtHIndex, nsHIndex, nameHIndex);
	}

      template<>
	inline typename str_to_long<kind::STRING>::type
	lookup<kind::STRING> (api_type<kind::LONG> ctxtHIndex,
			      api_type<kind::LONG> nsHIndex,
			      api_type<kind::LONG> nameHIndex)
	{
	  indexed<interned_string_handle> s
	    { lookup_intern<kind::STRING> (ctxtHIndex, nsHIndex, nameHIndex) };
	  return s.return_index ();
	}

      template<kind KIND>
	inline api_type<kind::BOOL>
	bind_in (api_type<kind::LONG> ctxtHIndex, api_type<kind::LONG> nsHIndex,
		 api_type<kind::LONG> nameHIndex,
		 typename str_to_long<KIND>::type val)
	{
	  indexed<iso_context_handle> ctxt
	    { ctxtHIndex };
	  indexed<namespace_handle> ns
	    { nsHIndex };
	  indexed<interned_string_handle> name
	    { nameHIndex };
	  api_type<KIND> v = val;
	  return ns->bind<KIND> (*ctxt, *name, v);
	}

      template<>
	inline api_type<kind::BOOL>
	bind_in<kind::STRING> (api_type<kind::LONG> ctxtHIndex,
			       api_type<kind::LONG> nsHIndex,
			       api_type<kind::LONG> nameHIndex,
			       typename str_to_long<kind::STRING>::type val)
	{
	  indexed<iso_context_handle> ctxt
	    { ctxtHIndex };
	  indexed<namespace_handle> ns
	    { nsHIndex };
	  indexed<interned_string_handle> name
	    { nameHIndex };
	  indexed<interned_string_handle> s
	    { val };
	  api_type<kind::STRING> v = *s;
	  return ns->bind<kind::STRING> (*ctxt, *name, v);
	}
    }
  }
}
