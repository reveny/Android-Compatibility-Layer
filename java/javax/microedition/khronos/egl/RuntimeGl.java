package javax.microedition.khronos.egl;

import android.opengl.GLES10;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

final class RuntimeGl implements GL10
{
    static final RuntimeGl INSTANCE = new RuntimeGl();

    private RuntimeGl()
    {
    }

    @Override
    public void glActiveTexture(int texture)
    {
        GLES10.glActiveTexture(texture);
    }

    @Override
    public void glAlphaFunc(int func, float ref)
    {
        GLES10.glAlphaFunc(func, ref);
    }

    @Override
    public void glAlphaFuncx(int func, int ref)
    {
        GLES10.glAlphaFuncx(func, ref);
    }

    @Override
    public void glBindTexture(int target, int texture)
    {
        GLES10.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor)
    {
        GLES10.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask)
    {
        GLES10.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha)
    {
        GLES10.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearColorx(int red, int green, int blue, int alpha)
    {
        GLES10.glClearColorx(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth)
    {
        GLES10.glClearDepthf(depth);
    }

    @Override
    public void glClearDepthx(int depth)
    {
        GLES10.glClearDepthx(depth);
    }

    @Override
    public void glClearStencil(int s)
    {
        GLES10.glClearStencil(s);
    }

    @Override
    public void glClientActiveTexture(int texture)
    {
        GLES10.glClientActiveTexture(texture);
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha)
    {
        GLES10.glColor4f(red, green, blue, alpha);
    }

    @Override
    public void glColor4x(int red, int green, int blue, int alpha)
    {
        GLES10.glColor4x(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha)
    {
        GLES10.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glColorPointer(int size, int type, int stride, Buffer pointer)
    {
        GLES10.glColorPointer(size, type, stride, pointer);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize,
            Buffer data)
    {
        GLES10.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
            int imageSize, Buffer data)
    {
        GLES10.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border)
    {
        GLES10.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height)
    {
        GLES10.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glCullFace(int mode)
    {
        GLES10.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(int n, int[] textures, int offset)
    {
        GLES10.glDeleteTextures(n, textures, offset);
    }

    @Override
    public void glDeleteTextures(int n, IntBuffer textures)
    {
        GLES10.glDeleteTextures(n, textures);
    }

    @Override
    public void glDepthFunc(int func)
    {
        GLES10.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag)
    {
        GLES10.glDepthMask(flag);
    }

    @Override
    public void glDepthRangef(float zNear, float zFar)
    {
        GLES10.glDepthRangef(zNear, zFar);
    }

    @Override
    public void glDepthRangex(int zNear, int zFar)
    {
        GLES10.glDepthRangex(zNear, zFar);
    }

    @Override
    public void glDisable(int cap)
    {
        GLES10.glDisable(cap);
    }

    @Override
    public void glDisableClientState(int array)
    {
        GLES10.glDisableClientState(array);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count)
    {
        GLES10.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices)
    {
        GLES10.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glEnable(int cap)
    {
        GLES10.glEnable(cap);
    }

    @Override
    public void glEnableClientState(int array)
    {
        GLES10.glEnableClientState(array);
    }

    @Override
    public void glFinish()
    {
        GLES10.glFinish();
    }

    @Override
    public void glFlush()
    {
        GLES10.glFlush();
    }

    @Override
    public void glFogf(int pname, float param)
    {
        GLES10.glFogf(pname, param);
    }

    @Override
    public void glFogfv(int pname, float[] params, int offset)
    {
        GLES10.glFogfv(pname, params, offset);
    }

    @Override
    public void glFogfv(int pname, FloatBuffer params)
    {
        GLES10.glFogfv(pname, params);
    }

    @Override
    public void glFogx(int pname, int param)
    {
        GLES10.glFogx(pname, param);
    }

    @Override
    public void glFogxv(int pname, int[] params, int offset)
    {
        GLES10.glFogxv(pname, params, offset);
    }

    @Override
    public void glFogxv(int pname, IntBuffer params)
    {
        GLES10.glFogxv(pname, params);
    }

    @Override
    public void glFrontFace(int mode)
    {
        GLES10.glFrontFace(mode);
    }

    @Override
    public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar)
    {
        GLES10.glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glFrustumx(int left, int right, int bottom, int top, int zNear, int zFar)
    {
        GLES10.glFrustumx(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glGenTextures(int n, int[] textures, int offset)
    {
        GLES10.glGenTextures(n, textures, offset);
    }

    @Override
    public void glGenTextures(int n, IntBuffer textures)
    {
        GLES10.glGenTextures(n, textures);
    }

    @Override
    public int glGetError()
    {
        return GLES10.glGetError();
    }

    @Override
    public void glGetIntegerv(int pname, int[] params, int offset)
    {
        GLES10.glGetIntegerv(pname, params, offset);
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params)
    {
        GLES10.glGetIntegerv(pname, params);
    }

    @Override
    public String glGetString(int name)
    {
        return GLES10.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode)
    {
        GLES10.glHint(target, mode);
    }

    @Override
    public void glLightModelf(int pname, float param)
    {
        GLES10.glLightModelf(pname, param);
    }

    @Override
    public void glLightModelfv(int pname, float[] params, int offset)
    {
        GLES10.glLightModelfv(pname, params, offset);
    }

    @Override
    public void glLightModelfv(int pname, FloatBuffer params)
    {
        GLES10.glLightModelfv(pname, params);
    }

    @Override
    public void glLightModelx(int pname, int param)
    {
        GLES10.glLightModelx(pname, param);
    }

    @Override
    public void glLightModelxv(int pname, int[] params, int offset)
    {
        GLES10.glLightModelxv(pname, params, offset);
    }

    @Override
    public void glLightModelxv(int pname, IntBuffer params)
    {
        GLES10.glLightModelxv(pname, params);
    }

    @Override
    public void glLightf(int light, int pname, float param)
    {
        GLES10.glLightf(light, pname, param);
    }

    @Override
    public void glLightfv(int light, int pname, float[] params, int offset)
    {
        GLES10.glLightfv(light, pname, params, offset);
    }

    @Override
    public void glLightfv(int light, int pname, FloatBuffer params)
    {
        GLES10.glLightfv(light, pname, params);
    }

    @Override
    public void glLightx(int light, int pname, int param)
    {
        GLES10.glLightx(light, pname, param);
    }

    @Override
    public void glLightxv(int light, int pname, int[] params, int offset)
    {
        GLES10.glLightxv(light, pname, params, offset);
    }

    @Override
    public void glLightxv(int light, int pname, IntBuffer params)
    {
        GLES10.glLightxv(light, pname, params);
    }

    @Override
    public void glLineWidth(float width)
    {
        GLES10.glLineWidth(width);
    }

    @Override
    public void glLineWidthx(int width)
    {
        GLES10.glLineWidthx(width);
    }

    @Override
    public void glLoadIdentity()
    {
        GLES10.glLoadIdentity();
    }

    @Override
    public void glLoadMatrixf(float[] m, int offset)
    {
        GLES10.glLoadMatrixf(m, offset);
    }

    @Override
    public void glLoadMatrixf(FloatBuffer m)
    {
        GLES10.glLoadMatrixf(m);
    }

    @Override
    public void glLoadMatrixx(int[] m, int offset)
    {
        GLES10.glLoadMatrixx(m, offset);
    }

    @Override
    public void glLoadMatrixx(IntBuffer m)
    {
        GLES10.glLoadMatrixx(m);
    }

    @Override
    public void glLogicOp(int opcode)
    {
        GLES10.glLogicOp(opcode);
    }

    @Override
    public void glMaterialf(int face, int pname, float param)
    {
        GLES10.glMaterialf(face, pname, param);
    }

    @Override
    public void glMaterialfv(int face, int pname, float[] params, int offset)
    {
        GLES10.glMaterialfv(face, pname, params, offset);
    }

    @Override
    public void glMaterialfv(int face, int pname, FloatBuffer params)
    {
        GLES10.glMaterialfv(face, pname, params);
    }

    @Override
    public void glMaterialx(int face, int pname, int param)
    {
        GLES10.glMaterialx(face, pname, param);
    }

    @Override
    public void glMaterialxv(int face, int pname, int[] params, int offset)
    {
        GLES10.glMaterialxv(face, pname, params, offset);
    }

    @Override
    public void glMaterialxv(int face, int pname, IntBuffer params)
    {
        GLES10.glMaterialxv(face, pname, params);
    }

    @Override
    public void glMatrixMode(int mode)
    {
        GLES10.glMatrixMode(mode);
    }

    @Override
    public void glMultMatrixf(float[] m, int offset)
    {
        GLES10.glMultMatrixf(m, offset);
    }

    @Override
    public void glMultMatrixf(FloatBuffer m)
    {
        GLES10.glMultMatrixf(m);
    }

    @Override
    public void glMultMatrixx(int[] m, int offset)
    {
        GLES10.glMultMatrixx(m, offset);
    }

    @Override
    public void glMultMatrixx(IntBuffer m)
    {
        GLES10.glMultMatrixx(m);
    }

    @Override
    public void glMultiTexCoord4f(int target, float s, float t, float r, float q)
    {
        GLES10.glMultiTexCoord4f(target, s, t, r, q);
    }

    @Override
    public void glMultiTexCoord4x(int target, int s, int t, int r, int q)
    {
        GLES10.glMultiTexCoord4x(target, s, t, r, q);
    }

    @Override
    public void glNormal3f(float nx, float ny, float nz)
    {
        GLES10.glNormal3f(nx, ny, nz);
    }

    @Override
    public void glNormal3x(int nx, int ny, int nz)
    {
        GLES10.glNormal3x(nx, ny, nz);
    }

    @Override
    public void glNormalPointer(int type, int stride, Buffer pointer)
    {
        GLES10.glNormalPointer(type, stride, pointer);
    }

    @Override
    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar)
    {
        GLES10.glOrthof(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glOrthox(int left, int right, int bottom, int top, int zNear, int zFar)
    {
        GLES10.glOrthox(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glPixelStorei(int pname, int param)
    {
        GLES10.glPixelStorei(pname, param);
    }

    @Override
    public void glPointSize(float size)
    {
        GLES10.glPointSize(size);
    }

    @Override
    public void glPointSizex(int size)
    {
        GLES10.glPointSizex(size);
    }

    @Override
    public void glPolygonOffset(float factor, float units)
    {
        GLES10.glPolygonOffset(factor, units);
    }

    @Override
    public void glPolygonOffsetx(int factor, int units)
    {
        GLES10.glPolygonOffsetx(factor, units);
    }

    @Override
    public void glPopMatrix()
    {
        GLES10.glPopMatrix();
    }

    @Override
    public void glPushMatrix()
    {
        GLES10.glPushMatrix();
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels)
    {
        GLES10.glReadPixels(x, y, width, height, format, type, pixels);
    }

    @Override
    public void glRotatef(float angle, float x, float y, float z)
    {
        GLES10.glRotatef(angle, x, y, z);
    }

    @Override
    public void glRotatex(int angle, int x, int y, int z)
    {
        GLES10.glRotatex(angle, x, y, z);
    }

    @Override
    public void glSampleCoverage(float value, boolean invert)
    {
        GLES10.glSampleCoverage(value, invert);
    }

    @Override
    public void glSampleCoveragex(int value, boolean invert)
    {
        GLES10.glSampleCoveragex(value, invert);
    }

    @Override
    public void glScalef(float x, float y, float z)
    {
        GLES10.glScalef(x, y, z);
    }

    @Override
    public void glScalex(int x, int y, int z)
    {
        GLES10.glScalex(x, y, z);
    }

    @Override
    public void glScissor(int x, int y, int width, int height)
    {
        GLES10.glScissor(x, y, width, height);
    }

    @Override
    public void glShadeModel(int mode)
    {
        GLES10.glShadeModel(mode);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask)
    {
        GLES10.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask)
    {
        GLES10.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass)
    {
        GLES10.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void glTexCoordPointer(int size, int type, int stride, Buffer pointer)
    {
        GLES10.glTexCoordPointer(size, type, stride, pointer);
    }

    @Override
    public void glTexEnvf(int target, int pname, float param)
    {
        GLES10.glTexEnvf(target, pname, param);
    }

    @Override
    public void glTexEnvfv(int target, int pname, float[] params, int offset)
    {
        GLES10.glTexEnvfv(target, pname, params, offset);
    }

    @Override
    public void glTexEnvfv(int target, int pname, FloatBuffer params)
    {
        GLES10.glTexEnvfv(target, pname, params);
    }

    @Override
    public void glTexEnvx(int target, int pname, int param)
    {
        GLES10.glTexEnvx(target, pname, param);
    }

    @Override
    public void glTexEnvxv(int target, int pname, int[] params, int offset)
    {
        GLES10.glTexEnvxv(target, pname, params, offset);
    }

    @Override
    public void glTexEnvxv(int target, int pname, IntBuffer params)
    {
        GLES10.glTexEnvxv(target, pname, params);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type,
            Buffer pixels)
    {
        GLES10.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param)
    {
        GLES10.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameterx(int target, int pname, int param)
    {
        GLES10.glTexParameterx(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
            Buffer pixels)
    {
        GLES10.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glTranslatef(float x, float y, float z)
    {
        GLES10.glTranslatef(x, y, z);
    }

    @Override
    public void glTranslatex(int x, int y, int z)
    {
        GLES10.glTranslatex(x, y, z);
    }

    @Override
    public void glVertexPointer(int size, int type, int stride, Buffer pointer)
    {
        GLES10.glVertexPointer(size, type, stride, pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height)
    {
        GLES10.glViewport(x, y, width, height);
    }
}
