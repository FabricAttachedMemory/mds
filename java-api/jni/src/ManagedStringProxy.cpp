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

/* C++ code implementing native methods of Java class:
 *   com.hpl.mds.impl.IntFieldProxy
 */

#include "mds-debug.h"
#include <string>
#include <jni.h>
#include "mds_core_api.h"                                // MDS Core API
#include "mds_jni.h"

using namespace mds::api;
using namespace mds::jni;

extern "C"
{

  JNIEXPORT
  void
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_release (JNIEnv *jEnv, jclass,
						    jlong handleIndex)
  {
    exception_handler (jEnv, [=]
      {
	indexed<interned_string_handle> self
	  { handleIndex};
	self.release();
      });
  }

  /* Class:     com_hpl_mds_impl_ManagedStringProxy
   * Method:    sameString
   * Signature: (JJ)Z
   */
  JNIEXPORT
  jboolean
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_sameString (JNIEnv *jEnv, jclass,
						       jlong aHIndex,
						       jlong bHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<interned_string_handle> a
	  { aHIndex};
	indexed<interned_string_handle> b
	  { bHIndex};
	return a==b;
      });
//  return handle_to_same<interned_string_handle>(aHIndex, bHIndex);
  }

  JNIEXPORT
  jint
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_compareTo (JNIEnv *jEnv, jclass,
						      jlong aHIndex,
						      jlong bHIndex)
  {
    return exception_handler_wr (jEnv, [=]
      {
	if (aHIndex == bHIndex)
	  {
	    return 0;
	  }
	indexed<interned_string_handle> a
	  { aHIndex};
	indexed<interned_string_handle> b
	  { bHIndex};
	auto ap = a->cbegin();
	auto ato = a->cend();
	auto bp = b->cbegin();
	auto bto = b->cend();
	for (; (ap != ato) && (bp != bto); ap++,bp++)
	  {
	    auto ac = *ap;
	    auto bc = *bp;
	    if (ac < bc)
	      {
		return -1;
	      }
	    if (ac > bc)
	      {
		return 1;
	      }
	  }
	if (ap == ato)
	  {
	    return -1;
	  }
	if (bp == bto)
	  {
	    return 1;
	  }
	return 0;
      });
  }

  /*
   * Class:     com_hpl_mds_impl_ManagedStringProxy
   * Method:    intern
   * Signature: (Ljava/lang/String;)J
   */
  JNIEXPORT
  jlong
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_intern (JNIEnv *jEnv, jclass,
						   jstring s)
  {
    if (s == nullptr)
      {
	return 0;
      }
    return exception_handler_wr (
	jEnv, [&]
	  {
	    using namespace std;

	    std::size_t len = jEnv->GetStringLength(s);

	    const jchar *chars = jEnv->GetStringCritical(s, nullptr);
	    if (chars == nullptr)
	      {
		return 0l; // exception thrown
      }
    static_assert(sizeof(jchar)==sizeof(char16_t), "jchar and char16_t different sizes");
    /*
     * TODO: erk: I have no idea why the static cast doesn't work.
     */
    const char16_t *wcs = reinterpret_cast<const char16_t *>(chars);
    //	const char16_t *wcs = static_cast<const char16_t *>(chars);
    /*
     * We can't use an indexed<ish> here, because it might block, and we're not
     * allowed to do that before calling ReleaseCritical.
     */
    interned_string_handle istring = intern(wcs, len);
    jEnv->ReleaseStringCritical(s, chars);
    indexed<interned_string_handle> msp
      { istring};
    return msp.return_index();
  });
  }

  /*
   * Class:     com_hpl_mds_impl_ManagedStringProxy
   * Method:    toString
   * Signature: (J)Ljava/lang/String;
   */
  JNIEXPORT
  jstring
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_toString (JNIEnv *jEnv, jclass,
						     jlong h)
  {
    using namespace std;
    if (h == 0)
      {
	return nullptr;
      }
    return exception_handler_wr (jEnv, [=]
      {
	indexed<interned_string_handle> is
	  { h};
	/*
	 * Unfortunately, there's no simple way to construct
	 * a java string from pair of iterators, so we actually
	 * wind up copying twice.  Sigh.
	 */
	basic_string<jchar> s
	  { is->cbegin(), is->cend()};
	return jEnv->NewString(s.c_str(), s.length());
      });
  }

  JNIEXPORT
  jint
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_length (JNIEnv *jEnv, jclass,
						   jlong h)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<interned_string_handle> is
	  { h};
	// TODO: What if a signed int isn't big enough?
				 return is->length();
			       });
  }

  JNIEXPORT
  jchar
  JNICALL
  Java_com_hpl_mds_impl_ManagedStringProxy_charAt (JNIEnv *jEnv, jclass,
						   jlong h, jint index)
  {
    return exception_handler_wr (jEnv, [=]
      {
	indexed<interned_string_handle> is
	  { h};
	std::size_t i = index;
	jchar c = (*is)[i];
	return c;
      });
  }
}
