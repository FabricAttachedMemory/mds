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

/*
 * core_fwd.h
 *
 *  Created on: Oct 21, 2014
 *      Author: evank
 */

#ifndef CORE_FWD_H_
#define CORE_FWD_H_

#include <cstdint>
#include <limits>
#include <atomic>
#include <type_traits>
#include <iostream>

#include "mds_types.h"
#include "ruts/managed.h"
#include "ruts/ms_forward.h"
#include "mpgc/gc.h"
#include "mpgc/gc_cuckoo_map.h"

namespace mds {
  namespace core {
    using namespace mpgc;

    using ruts::with_uniform_id;
    using ruts::uniform_key;

    enum class kind {
      BOOL,
      BYTE, UBYTE,
      SHORT, USHORT,
      INT, UINT,
      LONG, ULONG,
      FLOAT, DOUBLE,
      STRING,
      RECORD,
      BINDING,
      ARRAY,
      n_kinds
    };
    constexpr std::size_t n_kinds = static_cast<std::size_t>(kind::n_kinds);

    using timestamp_t = std::size_t;

    constexpr timestamp_t most_recent = std::numeric_limits<timestamp_t>::max();
    class unimplemented {};
    class control;

    using current_version_t = std::atomic<std::uint64_t>;
    class exportable : public gc_allocated {
      using gc_allocated::gc_allocated;
    };

    template <typename T, typename = void> struct is_exportable : std::false_type {};

    template <typename T>
    struct is_exportable<T, std::enable_if_t<std::is_base_of<exportable, T>::value>>
    : std::true_type
      {};

    class branch;

    /*
     * parent_view is used in merging.  If C extends P, a parent view child of C sees the parent
     * branch of whatever branch C would see.
     */
    enum class view_type : unsigned char {
      live, snapshot, parent_view
    };

    enum class mod_type : unsigned char {
      full, detached, read_only
    };

    class iso_context;
    class snapshot;
    template <kind K> class record_field;
    class managed_type_base;
    template <kind K> class managed_type;
    class record_type;
    class conflict;
    class conflict_list;
    class interned_string;
    template <std::size_t S> class interned_string_table;

    class managed_object : public exportable {
    public:
      using exportable::exportable;
    };
    class branch_dependent_value : public managed_object {
    public:
      using managed_object::managed_object;
    };
    class managed_composite : public branch_dependent_value {
    public:
      explicit managed_composite(gc_token &gc) : branch_dependent_value{gc} {}
    };

    class managed_record;
    class managed_container;
    class managed_map;
    class managed_collection;
    class managed_list;
    class managed_set;

    class array_type_base;
    template <kind K> class array_type;
    class managed_array_base;
    template <kind K> class managed_array;

    // su - this avoids having a cyclical include between core_array and core_context
    using array_index_type = std::ptrdiff_t;

    template <typename T>
    constexpr bool is_value_type() {
      return std::is_arithmetic<T>() || std::is_base_of<managed_object,T>();
    }


    class incompatible_type_ex {};
    class incompatible_superclass_ex {};
    class unmodifiable_record_type_ex {};
    class incompatible_record_type_ex {
    public:
//      incompatible_record_type_ex() {
//        std::cerr << "Throwing incompatible_record_type_ex" << std::endl;
//      }
    };
    class unbound_name_ex {};
    class read_only_context_ex {};
    class unmergeable_context_ex {};

    template <kind K> struct kind_traits;
    template <kind K> using kind_val = typename kind_traits<K>::val_type;
    template <kind K> using kind_type = typename kind_traits<K>::type_t;
    template <kind K> using kind_field = typename kind_traits<K>::field_t;

    static constexpr std::size_t n_string_table_segments = 2;
    static constexpr std::size_t initial_string_table_capacity = 10000;
    using string_table_t = interned_string_table<n_string_table_segments>;
    template <> struct is_exportable<interned_string> : std::true_type {};

    class msv_base;
    template <kind K> class msv;

    class name_space;
    class binding;

    static constexpr std::size_t initial_record_type_table_capacity = 1000;
    using record_type_table_t = gc_cuckoo_map<gc_ptr<interned_string>, gc_ptr<const record_type>,
                                              ruts::hash1<gc_ptr<interned_string>>,
                                              ruts::hash2<gc_ptr<interned_string>>, 5>;



    template <typename T, typename Enable = void>
    struct managed_value_wrapper {
      using type = gc_ptr<T>;
    };
    template <typename T> using managed_value = typename managed_value_wrapper<T>::type;
    template <kind K> using kind_mv = managed_value<kind_val<K>>;

    template <typename T>
    struct managed_value_wrapper<T, std::enable_if_t<std::is_arithmetic<T>::value>>
    {
      using type = T;
    };

    template <>
    struct managed_value_wrapper<binding> {
      using type = binding;
    };

    template <typename T>
    struct bd_value {
      gc_ptr<T> value;
      gc_ptr<branch> on_branch;
      bd_value(const gc_ptr<T> &v, const gc_ptr<branch> &b) : value{v}, on_branch{b} {
        assert( (value == nullptr) == (on_branch == nullptr) );
      }
      bd_value() : value{nullptr}, on_branch{nullptr} {}
      static const auto &descriptor() {
        static gc_descriptor d =
	  GC_DESC(bd_value)
	  .template WITH_FIELD(&bd_value::value)
	  .template WITH_FIELD(&bd_value::on_branch);
        return d;
      }
    };

    template <typename T>
    struct managed_value_wrapper<T, std::enable_if_t<std::is_base_of<managed_composite, T>::value>>
    {
      using type = bd_value<T>;
    };

    enum class validity : unsigned char {
      unchecked, valid, invalid
    };

    template <typename Fn>
    inline
    bool check_validity(validity &v, Fn &&fn) {
      if (v == validity::unchecked) {
        v = std::forward<Fn>(fn)() ? validity::valid : validity::invalid;
      }
      return v == validity::valid;
    }

  }
}

namespace std {
  template <typename C, typename Tr, typename T>
  basic_ostream<C,Tr> &
  operator <<(basic_ostream<C,Tr> &os, const mds::core::bd_value<T> &v) {
    return os << v.value << "{" << v.on_branch << "}";
  }
}



#endif /* CORE_FWD_H_ */
