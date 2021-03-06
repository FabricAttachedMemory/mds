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
 * core_coop.h
 *
 *  Created on: Nov 5, 2014
 *      Author: evank
 *
 *  A cooperative_task is something that has to be done before something else can be done,
 *  and any thread that encounters one must assist in finishing it before proceeding.
 *
 */

#ifndef CORE_COOP_H_
#define CORE_COOP_H_

#include "ruts/cas_loop.h"
#include "ruts/meta.h"
#include "core/core_fwd.h"
#include "mpgc/gc_virtuals.h"
#include <atomic>
#include <memory>

namespace mds {
  namespace core {
    template <typename Disc = std::size_t>
    class cooperative_task : public gc_allocated_with_virtuals<cooperative_task<Disc>, Disc> {
    public:
      using super = gc_allocated_with_virtuals<cooperative_task<Disc>, Disc>;
      using typename super::discriminator_type;

      struct virtuals : super::virtuals_base {
	virtual void run(cooperative_task *self) = 0;
      };

      void run() {
	this->call_virtual(this, &virtuals::run);
      }

      using atomic_holder = std::atomic<gc_ptr<cooperative_task>>;

      explicit cooperative_task(gc_token &gc, discriminator_type d) : super(gc,d) {}

      static const auto &descriptor() {
	static gc_descriptor d =
	  GC_DESC(cooperative_task)
	  .template WITH_SUPER(super);
	return d;
      }

      void install(atomic_holder &place) {
	while (true) {
	  auto rr = ruts::try_change_value(place, nullptr, super::this_as_gc_ptr(this));
	  if (rr) {
	    return;
	  }
	  /*
	   * There was already one there.
	   */
	  gc_ptr<cooperative_task> current = rr.prior_value;
	  if (current == this) {
	    // Two threads tried to install the same event (as part of its execution)
	    return;
	  }
	  current->run_and_remove(place);
	}
      }

      void run_and_remove(atomic_holder &place) {
	run();
	ruts::try_change_value(place, super::this_as_gc_ptr(this), nullptr);
	/*
	 * If that fails, it means that somebody removed it first.
	 */
      }

      void install_and_run(atomic_holder &place) {
	install(place);
	run_and_remove(place);
      }
    };
    
    template <typename State, State init, State final>
    class task_run_state {
      std::atomic<State> _state{init};
	State next_state(State s) {
	  State ns = static_cast<State>(static_cast<std::size_t>(s)+1);
	  auto rr = ruts::try_change_value(_state, s, ns);
	  /*
	   * If this fails, it means somebody else got there first.  Use the result in any case
	   */
	  return rr.resulting_value();
	}
    public:
      static const auto &descriptor() {
	static gc_descriptor d =
	  GC_DESC(task_run_state)
	  .template WITH_FIELD(&task_run_state::_state);
	return d;
      }

      State current() const {
        return _state;
      }
      
      template <typename T, typename X, typename Y>
      void run(T *obj, X (T::*dispatch)(Y)) {
	for (State s = _state; s < final; s = next_state(s)) {
	  (obj->*dispatch)(s);
	}
      }
    };
    

#if 0
    template <typename Base, typename State, State init, State final>
    class state_based_task : public Base {
      using super = Base;
      std::atomic<State> _state{init};

      State next_state(State s) {
	State ns = static_cast<State>(static_cast<std::size_t>(s)+1);
	auto rr = ruts::try_change_value(_state, s, ns);
	/*
	 * If this fails, it means somebody else got there first.  Use the result in any case
	 */
	return rr.resulting_value();
      }
    protected:
      state_based_task(gc_token &gc, typename Base::discriminator_type d) : super{gc, d} {}
    public:
      static const auto &descriptor() {
	static gc_descriptor d =
	  GC_DESC(state_based_task)
	  .WITH_FIELD&state_based_task_data::_state);
	return d;
      }
      template <typename Fn>
      void run_states(Fn&& dispatch)  {
	if (!super::is_done()) {
	  for (State s = _state; s < final; s = next_state(s)) {
	    b.dispatch(s, static_cast<typename Behavior::data_type &>(*this));
	  }
	  super::set_done();
	}
      }

    };
#endif    
  }
}





#endif /* CORE_COOP_H_ */
